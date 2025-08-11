package com.fab.video_convert_platform.service.impl;

import com.fab.video_convert_platform.common.BusinessException;
import com.fab.video_convert_platform.common.ErrorCode;
import com.fab.video_convert_platform.common.VideoConstants;
import com.fab.video_convert_platform.domain.ProjectConfig;
import com.fab.video_convert_platform.domain.VideoUploadTask;
import com.fab.video_convert_platform.domain.repository.ProjectConfigRepository;
import com.fab.video_convert_platform.domain.repository.VideoUploadTaskRepository;
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

/**
 * Implementation of video service operations with optimized transaction boundaries.
 */
@Service
public class VideoServiceImpl implements IVideoService {

    private final ProjectConfigRepository projectConfigRepository;
    private final VideoUploadTaskRepository uploadTaskRepository;
    private final NfsService nfsService;
    private final ITaskLogService taskLogService;
    private final LocalSliceTaskExecutor sliceTaskExecutor;
    private final Tracer tracer;
    private final IUploadTaskTxService uploadTaskTxService;

    public VideoServiceImpl(ProjectConfigRepository projectConfigRepository,
                            VideoUploadTaskRepository uploadTaskRepository,
                            NfsService nfsService,
                            ITaskLogService taskLogService,
                            LocalSliceTaskExecutor sliceTaskExecutor,
                            Tracer tracer,
                            IUploadTaskTxService uploadTaskTxService) {
        this.projectConfigRepository = projectConfigRepository;
        this.uploadTaskRepository = uploadTaskRepository;
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
                    path, file.getSize(), md5);

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
                            String tpStage, String uuid) {
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
                        filename, target, size, md5);

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
                String fileName = source.getFileName().toString();
                int versionNo = VideoConstants.DEFAULT_VERSION_NO;
                String uuid = UUID.randomUUID().toString().replace("-", "");
                Path target = ArchivePathUtil.buildOriginalPath(config.getArchiveRoot(),
                        message.getProjectNo(), message.getPatientCode(), message.getTpStage(),
                        versionNo, uuid, fileName);

                nfsService.copyFile(source, target);
                long size = Files.size(target);

                // 5. 数据库操作（使用事务）
                VideoUploadTask task = uploadTaskTxService.saveUploadTaskInTransaction(message.getProjectNo(),
                        message.getPatientCode(), message.getTpStage(), uuid, versionNo,
                        VideoConstants.SOURCE_MQ, fileName, target, size, md5);

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
        ProjectConfig config = projectConfigRepository.findByProjectNo(projectNo).orElse(null);
        if (config == null) {
            throw new BusinessException(ErrorCode.PROJECT_NOT_FOUND,
                "Project not found: " + projectNo);
        }
        return config;
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
}
