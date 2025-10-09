package com.fab.video_convert_platform.maintenance;

import com.fab.video_convert_platform.domain.ProjectConfig;
import com.fab.video_convert_platform.domain.repository.ProjectConfigRepository;
import com.fab.video_convert_platform.domain.repository.VideoUploadTaskRepository;
import com.fab.video_convert_platform.infra.NfsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 定期清理：
 * 1. 孤立的分片目录：{archiveRoot}/{project}/{patient}/{tp}/{version}/{uuid}/chunk
 *    - 超过配置 TTL 未被删除（意味着合并异常或任务失败未清理）
 * 2. 无任务记录的原始文件：.../{uuid}/original/* 但数据库无对应 uuid 任务，且超过 TTL
 *
 * Dry-run 模式下仅打印日志不删除，便于上线前验证。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrphanFileCleanupTask {

    private final CleanupProperties properties;
    private final ProjectConfigRepository projectConfigRepository;
    private final VideoUploadTaskRepository taskRepository;
    private final NfsService nfsService;

    private final AtomicBoolean running = new AtomicBoolean(false);

    @Scheduled(cron = "${maintenance.cleanup.cron:0 0/30 * * * ?}")
    public void execute() {
        if (!properties.isEnabled()) {
            return; // 已关闭
        }
        if (!running.compareAndSet(false, true)) {
            log.warn("清理任务仍在运行，跳过本次调度");
            return;
        }
        long start = System.currentTimeMillis();
        int chunkDirsDeleted = 0;
        int originalDirsDeleted = 0;
        try {
            List<ProjectConfig> configs = projectConfigRepository.findAllActive();
            for (ProjectConfig config : configs) {
                String archiveRoot = config.getArchiveRoot();
                // Java 8 兼容的空白判断
                if (archiveRoot == null || archiveRoot.trim().isEmpty()) {
                    continue;
                }
                Path root = Paths.get(archiveRoot);
                if (!Files.exists(root) || !Files.isDirectory(root)) {
                    continue;
                }
                // 遍历项目下第一级：projectNo -> 再深度遍历
                try (DirectoryStream<Path> projectDirs = Files.newDirectoryStream(root)) {
                    for (Path projectDir : projectDirs) {
                        if (!Files.isDirectory(projectDir)) continue;
                        scanProjectDirectory(projectDir);
                    }
                } catch (IOException e) {
                    log.warn("扫描归档根目录失败: root={}, error={}", root, e.getMessage());
                }
            }
            chunkDirsDeleted = deletedChunkCount;
            originalDirsDeleted = deletedOriginalCount;
        } finally {
            running.set(false);
            log.info("孤立清理任务完成: 用时={}ms, 删除chunk目录={}, 删除无任务original目录={}, dryRun={}",
                System.currentTimeMillis() - start, chunkDirsDeleted, originalDirsDeleted, properties.isDryRun());
            // 重置计数
            deletedChunkCount = 0;
            deletedOriginalCount = 0;
        }
    }

    private int deletedChunkCount = 0;
    private int deletedOriginalCount = 0;

    private void scanProjectDirectory(Path projectDir) {
        // 结构: projectDir/patientCode/tpStage/version/uuid
        try (DirectoryStream<Path> patientDirs = Files.newDirectoryStream(projectDir)) {
            for (Path patientDir : patientDirs) {
                if (!Files.isDirectory(patientDir)) continue;
                try (DirectoryStream<Path> tpDirs = Files.newDirectoryStream(patientDir)) {
                    for (Path tpDir : tpDirs) {
                        if (!Files.isDirectory(tpDir)) continue;
                        try (DirectoryStream<Path> versionDirs = Files.newDirectoryStream(tpDir)) {
                            for (Path versionDir : versionDirs) {
                                if (!Files.isDirectory(versionDir)) continue;
                                try (DirectoryStream<Path> uuidDirs = Files.newDirectoryStream(versionDir)) {
                                    for (Path uuidDir : uuidDirs) {
                                        if (!Files.isDirectory(uuidDir)) continue;
                                        handleUuidDirectory(uuidDir);
                                    }
                                } catch (IOException e) {
                                    log.debug("扫描uuid目录失败: path={}, error={}", versionDir, e.getMessage());
                                }
                            }
                        } catch (IOException e) {
                            log.debug("扫描version目录失败: path={}, error={}", tpDir, e.getMessage());
                        }
                    }
                } catch (IOException e) {
                    log.debug("扫描tp目录失败: path={}, error={}", patientDir, e.getMessage());
                }
            }
        } catch (IOException e) {
            log.debug("扫描patient目录失败: path={}, error={}", projectDir, e.getMessage());
        }
    }

    private void handleUuidDirectory(Path uuidDir) {
        String uuid = uuidDir.getFileName() != null ? uuidDir.getFileName().toString() : null;
        if (uuid == null) return;
        boolean existsInDb = taskRepository.existsByUuid(uuid);

        Path chunkDir = uuidDir.resolve("chunk");
        Path originalDir = uuidDir.resolve("original");

        Instant now = Instant.now();
        // 处理 chunk 目录
        if (Files.exists(chunkDir) && Files.isDirectory(chunkDir)) {
            try {
                Instant last = computeLastModified(chunkDir);
                long ageMinutes = Duration.between(last, now).toMinutes();
                if (ageMinutes >= properties.getOrphanChunkTtlMinutes()) {
                    // 如果任务已经存在，说明是残留；如果任务不存在且也没有 original，说明上传中断
                    boolean hasOriginal = Files.exists(originalDir);
                    if (existsInDb || !hasOriginal) {
                        deletePath(chunkDir, "orphan-chunk", uuid, ageMinutes + "m");
                        deletedChunkCount++;
                    }
                }
            } catch (IOException e) {
                log.debug("计算chunk目录最近修改时间失败: path={}, error={}", chunkDir, e.getMessage());
            }
        }

        // 处理 original 目录：仅当 DB 无记录且超过 TTL
        if (!existsInDb && Files.exists(originalDir) && Files.isDirectory(originalDir)) {
            try {
                Instant last = computeLastModified(originalDir);
                long ageHours = Duration.between(last, now).toHours();
                if (ageHours >= properties.getOrphanOriginalTtlHours()) {
                    // 删除整个 uuid 目录，避免残留结构
                    deletePath(uuidDir, "orphan-original", uuid, ageHours + "h");
                    deletedOriginalCount++;
                }
            } catch (IOException e) {
                log.debug("计算original目录最近修改时间失败: path={}, error={}", originalDir, e.getMessage());
            }
        }
    }

    private Instant computeLastModified(Path dir) throws IOException {
        final Instant[] last = {Instant.EPOCH};
        Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Instant lm = attrs.lastModifiedTime().toInstant();
                if (lm.isAfter(last[0])) last[0] = lm;
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult preVisitDirectory(Path d, BasicFileAttributes attrs) throws IOException {
                Instant lm = attrs.lastModifiedTime().toInstant();
                if (lm.isAfter(last[0])) last[0] = lm;
                return FileVisitResult.CONTINUE;
            }
        });
        return last[0];
    }

    private void deletePath(Path path, String type, String uuid, String ageDesc) {
        if (properties.isDryRun()) {
            log.info("[DRY-RUN] 发现{}待删除: uuid={}, path={}, age={} (未实际删除)", type, uuid, path, ageDesc);
            return;
        }
        try {
            nfsService.deleteRecursively(path);
            log.info("删除{}成功: uuid={}, path={}, age={}", type, uuid, path, ageDesc);
        } catch (IOException e) {
            log.warn("删除{}失败: uuid={}, path={}, error={}", type, uuid, path, e.getMessage());
        }
    }
}
