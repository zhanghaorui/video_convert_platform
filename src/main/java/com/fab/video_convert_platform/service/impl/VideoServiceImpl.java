package com.fab.video_convert_platform.service.impl;

import com.fab.video_convert_platform.common.BusinessException;
import com.fab.video_convert_platform.common.ErrorCode;
import com.fab.video_convert_platform.common.VideoConstants;
import lombok.extern.slf4j.Slf4j;
import com.fab.video_convert_platform.domain.ProjectConfig;
import com.fab.video_convert_platform.domain.VideoUploadTask;
import com.fab.video_convert_platform.domain.VideoArchiveFile;
import com.fab.video_convert_platform.domain.repository.ProjectConfigRepository;
import com.fab.video_convert_platform.domain.repository.VideoUploadTaskRepository;
import com.fab.video_convert_platform.domain.repository.VideoArchiveFileRepository;
import com.fab.video_convert_platform.service.dto.MqVideoMessage;
import com.fab.video_convert_platform.service.IVideoService;
import com.fab.video_convert_platform.service.ITaskLogService;
import com.fab.video_convert_platform.service.IUploadTaskTxService;
import com.fab.video_convert_platform.infra.NfsService;
import com.fab.video_convert_platform.infra.LocalSliceTaskExecutor;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import com.fab.video_convert_platform.util.ArchivePathUtil;
import com.fab.video_convert_platform.util.DigestUtil;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.List;
import java.util.Collections;
import java.util.stream.Collectors;

/**
 * Implementation of video service operations with optimized transaction boundaries.
 */
@Slf4j
@Service
public class VideoServiceImpl implements IVideoService {

    @SuppressWarnings("EI_EXPOSE_REP2") // 依赖注入场景，预期行为
    private final ProjectConfigRepository projectConfigRepository;
    @SuppressWarnings("EI_EXPOSE_REP2") // 依赖注入场景，预期行为
    private final VideoUploadTaskRepository uploadTaskRepository;
    @SuppressWarnings("EI_EXPOSE_REP2") // 依赖注入场景，预期行为
    private final VideoArchiveFileRepository archiveFileRepository;
    @SuppressWarnings("EI_EXPOSE_REP2") // 依赖注入场景，预期行为
    private final NfsService nfsService;
    @SuppressWarnings("EI_EXPOSE_REP2") // 依赖注入场景，预期行为
    private final ITaskLogService taskLogService;
    @SuppressWarnings("EI_EXPOSE_REP2") // 依赖注入场景，预期行为
    private final LocalSliceTaskExecutor sliceTaskExecutor;
    @SuppressWarnings("EI_EXPOSE_REP2") // 依赖注入场景，预期行为
    private final Tracer tracer;
    @SuppressWarnings("EI_EXPOSE_REP2") // 依赖注入场景，预期行为
    private final IUploadTaskTxService uploadTaskTxService;

    public VideoServiceImpl(ProjectConfigRepository projectConfigRepository,
                            VideoUploadTaskRepository uploadTaskRepository,
                            VideoArchiveFileRepository archiveFileRepository,
                            NfsService nfsService,
                            ITaskLogService taskLogService,
                            LocalSliceTaskExecutor sliceTaskExecutor,
                            Tracer tracer,
                            IUploadTaskTxService uploadTaskTxService) {
        this.projectConfigRepository = projectConfigRepository;
        this.uploadTaskRepository = uploadTaskRepository;
        this.archiveFileRepository = archiveFileRepository;
        this.nfsService = nfsService;
        this.taskLogService = taskLogService;
        this.sliceTaskExecutor = sliceTaskExecutor;
        this.tracer = tracer;
        this.uploadTaskTxService = uploadTaskTxService;
    }

    @Override
    public VideoUploadTask upload(MultipartFile file, String projectNo,
                                  String patientCode, String tpStage) {
        Span span = tracer.nextSpan().name("ingest_receive").start();
        span.tag("project_no", projectNo);
        span.tag("source", VideoConstants.SOURCE_CONTROLLER);
        try (Tracer.SpanInScope ws = tracer.withSpan(span)) {
            // 1. 验证项目配置（不需要事务）
            ProjectConfig config = validateProject(projectNo);

            // 2. 准备任务信息
            String uuid = UUID.randomUUID().toString().replace("-", "");
            int versionNo = VideoConstants.DEFAULT_VERSION_NO;
            String fileName = file.getOriginalFilename();
            Path path = ArchivePathUtil.buildOriginalPath(config.getArchiveRoot(),
                    projectNo, patientCode, tpStage, versionNo, uuid, fileName);

            // 3. 文件存储（不需要事务）
            VideoUploadTask task;
            try {
                nfsService.saveFile(file, path);
                String md5 = DigestUtil.md5(path);

                // 4. 数据库操作（使用事务）
                task = uploadTaskTxService.saveUploadTaskInTransaction(projectNo, patientCode, tpStage,
                    uuid, versionNo, VideoConstants.SOURCE_CONTROLLER, fileName,
                    path, file.getSize(), md5, null); // visit null for full upload

                span.tag("task_id", String.valueOf(task.getId()));
                taskLogService.info(task.getId(), "original file archived");
            } catch (IOException e) {
                span.error(e);
                throw new BusinessException(ErrorCode.STORE_FILE_FAILED,
                    "Failed to store file: " + e.getMessage());
            }

            // 5. 异步处理视频切片（事务外执行）
            processVideoAsync(config, task);

            return uploadTaskRepository.findById(task.getId()).orElse(task);
        } finally {
            span.end();
        }
    }

    @Override
    public void uploadChunk(MultipartFile file, Integer chunk, Integer chunks,
                            String filename, String projectNo, String patientCode,
                            String tpStage, String uuid, String visit) { // add visit
        // 1. 验证项目配置
        ProjectConfig config = validateProject(projectNo);

        int versionNo = VideoConstants.DEFAULT_VERSION_NO;
        Path chunkDir = ArchivePathUtil.buildChunkPath(config.getArchiveRoot(),
                projectNo, patientCode, tpStage, versionNo, uuid);

        try {
            // 2. 保存分片文件
            nfsService.saveChunk(file, chunkDir, chunk == null ? 0 : chunk);

            // 3. 检查是否为最后一个分片
            if (chunk != null && chunks != null && chunk + 1 == chunks) {
                Span span = tracer.nextSpan().name("ingest_receive").start();
                span.tag("project_no", projectNo);
                span.tag("source", VideoConstants.SOURCE_CONTROLLER);
                try (Tracer.SpanInScope ws = tracer.withSpan(span)) {
                    // 合并分片
                    Path target = ArchivePathUtil.buildOriginalPath(config.getArchiveRoot(),
                            projectNo, patientCode, tpStage, versionNo, uuid, filename);
                    nfsService.mergeChunks(chunkDir, target, chunks);

                    long size = Files.size(target);
                    String md5 = DigestUtil.md5(target);

                    // 4. 数据库操作（使用事务）
                    VideoUploadTask task = uploadTaskTxService.saveUploadTaskInTransaction(projectNo, patientCode,
                        tpStage, uuid, versionNo, VideoConstants.SOURCE_CONTROLLER,
                        filename, target, size, md5, visit);

                    span.tag("task_id", String.valueOf(task.getId()));
                    taskLogService.info(task.getId(), "chunks merged and archived");

                    // 5. 清理分片目录
                    nfsService.deleteRecursively(chunkDir);

                    // 6. 异步处理视频切片（事务外执行）
                    processVideoAsync(config, task);
                } finally {
                    span.end();
                }
            }
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.CHUNK_MERGE_FAILED,
                "Failed to process chunk: " + e.getMessage());
        }
    }

    @Override
    public void processMqMessage(MqVideoMessage message) {
        Span span = tracer.nextSpan().name("ingest_receive").start();
        span.tag("project_no", message.getProjectNo());
        span.tag("source", VideoConstants.SOURCE_MQ);
        try (Tracer.SpanInScope ws = tracer.withSpan(span)) {
            // 1. 验证项目配置
            ProjectConfig config = validateProject(message.getProjectNo());

            // 2. 验证源文件
            Path source = Paths.get(message.getFilePath());
            if (!Files.exists(source)) {
                throw new BusinessException(ErrorCode.SOURCE_FILE_NOT_FOUND,
                    "Source file not found: " + message.getFilePath());
            }

            // 2.1 处理 tpStage：如果为空则使用 visit 作为默认值
            String tpStage = message.getTpStage();
            if (tpStage == null || tpStage.trim().isEmpty()) {
                tpStage = message.getVisit() != null ? message.getVisit() : "UNKNOWN";
                log.info("tpStage为空，使用 visit={} 或默认值: {}", message.getVisit(), tpStage);
            }

            try {
                // 3. MD5校验
                if (message.getFileMd5() == null) {
                    throw new BusinessException(ErrorCode.MD5_REQUIRED,
                        "MD5 is required for MQ message");
                }

                String md5 = DigestUtil.md5(source);
                if (!message.getFileMd5().equalsIgnoreCase(md5)) {
                    throw new BusinessException(ErrorCode.MD5_MISMATCH,
                        "MD5 verification failed");
                }

                // 4. 复制文件到归档目录
                Path fileNamePath = source.getFileName();
                if (fileNamePath == null) {
                    throw new BusinessException(ErrorCode.MQ_PROCESS_FAILED, "Invalid source path");
                }
                String fileName = fileNamePath.toString();
                int versionNo = VideoConstants.DEFAULT_VERSION_NO;
                String uuid = UUID.randomUUID().toString().replace("-", "");
                Path target = ArchivePathUtil.buildOriginalPath(config.getArchiveRoot(),
                        message.getProjectNo(), message.getPatientCode(), tpStage,
                        versionNo, uuid, fileName);

                nfsService.copyFile(source, target);
                long size = Files.size(target);

                // 5. 数据库操作（使用事务）- 传递 visit 元数据
                VideoUploadTask task = uploadTaskTxService.saveUploadTaskInTransaction(message.getProjectNo(),
                        message.getPatientCode(), tpStage, uuid, versionNo,
                        VideoConstants.SOURCE_MQ, fileName, target, size, md5, message.getVisit());

                span.tag("task_id", String.valueOf(task.getId()));
                taskLogService.info(task.getId(), "mq file archived");

                // 6. 异步处理视频切片（事务外执行）
                processVideoAsync(config, task);

            } catch (IOException e) {
                span.error(e);
                throw new BusinessException(ErrorCode.MQ_PROCESS_FAILED,
                    "Failed to process MQ file: " + e.getMessage());
            }
        } finally {
            span.end();
        }
    }

    /**
     * 验证项目配置
     */
    private ProjectConfig validateProject(String projectNo) {
        return projectConfigRepository.findByProjectNo(projectNo)
            .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND,
                "Project not found: " + projectNo));
    }


    /**
     * 异步处理视频切片（事务外执行）
     */
    private void processVideoAsync(ProjectConfig config, VideoUploadTask task) {
        sliceTaskExecutor.submit(config, task);
    }

    @Override
    public VideoUploadTask getTaskById(Long taskId) {
        if (taskId == null || taskId <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "任务ID无效");
        }

        VideoUploadTask task = uploadTaskRepository.findById(taskId).orElse(null);
        if (task == null) {
            throw new BusinessException(ErrorCode.TASK_NOT_FOUND,
                "任务不存在: " + taskId);
        }

        return task;
    }

    @Override
    public List<VideoArchiveFile> getPlayUrlsByTaskId(Long taskId) {
        if (taskId == null || taskId <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "任务ID无效");
        }

        // 验证任务是否存在
        getTaskById(taskId);

        // 查询该任务的所有M3U8归档文件（这些文件包含播放URL）
        List<VideoArchiveFile> archiveFiles = archiveFileRepository.findByTaskIdAndFileType(taskId, VideoConstants.FILE_TYPE_M3U8);
        
        // 只返回状态为READY的播放文件
        return archiveFiles.stream()
                .filter(file -> "READY".equals(file.getStatus()))
                .collect(Collectors.toList());
    }

    @Override
    public List<VideoArchiveFile> getPlayUrlsByParams(String projectNo, String patientCode, 
                                                      String tpStage, String visit, Integer versionNo, String quality) {
        if (projectNo == null || projectNo.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "项目编号不能为空");
        }
        if (patientCode == null || patientCode.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "受试者编码不能为空");
        }
        // 互斥校验：必须且只能传一个 tpStage 或 visit
        boolean hasTp = tpStage != null && !tpStage.trim().isEmpty();
        boolean hasVisit = visit != null && !visit.trim().isEmpty();
        if (hasTp == hasVisit) { // 同时为true或同时为false
            throw new BusinessException(ErrorCode.PARAM_ERROR, "tpStage与visit必须且只能传一个");
        }

        List<VideoUploadTask> tasks;
        if (hasVisit) {
            tasks = uploadTaskRepository.findByProjectAndPatientAndVisit(projectNo, patientCode, visit);
        } else {
            tasks = uploadTaskRepository.findByProjectAndPatientAndStage(projectNo, patientCode, tpStage);
        }

        if (tasks.isEmpty()) {
            return Collections.emptyList();
        }

        // 如果指定版本号过滤
        if (versionNo != null) {
            tasks = tasks.stream()
                    .filter(task -> versionNo.equals(task.getVersionNo()))
                    .collect(Collectors.toList());
            if (tasks.isEmpty()) {
                return Collections.emptyList();
            }
        }

        // 收集所有任务的 M3U8 播放文件
        List<VideoArchiveFile> allPlayUrls = tasks.stream()
                .flatMap(task -> archiveFileRepository
                        .findByTaskIdAndFileType(task.getId(), VideoConstants.FILE_TYPE_M3U8)
                        .stream())
                .filter(file -> "READY".equals(file.getStatus()))
                .collect(Collectors.toList());

        if (quality != null && !quality.trim().isEmpty()) {
            // 映射质量参数：standard -> normal
            String mappedQuality = mapQualityParam(quality);
            allPlayUrls = allPlayUrls.stream()
                    .filter(file -> mappedQuality.equals(file.getQualityLevel()))
                    .collect(Collectors.toList());
        }

        return allPlayUrls;
    }

    /**
     * 映射质量参数，将用户传入的quality参数映射为数据库存储的quality_level
     * @param quality 用户传入的质量参数
     * @return 映射后的质量级别
     */
    private String mapQualityParam(String quality) {
        if ("standard".equals(quality)) {
            return VideoConstants.QUALITY_NORMAL; // "normal"
        }
        return quality;
    }
}
