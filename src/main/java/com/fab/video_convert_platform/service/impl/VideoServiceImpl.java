package com.fab.video_convert_platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fab.video_convert_platform.common.BusinessException;
import com.fab.video_convert_platform.common.ErrorCode;
import com.fab.video_convert_platform.common.VideoConstants;
import com.fab.video_convert_platform.domain.ProjectConfig;
import com.fab.video_convert_platform.domain.VideoUploadTask;
import com.fab.video_convert_platform.service.dto.MqVideoMessage;
import com.fab.video_convert_platform.domain.service.VideoTaskDomainService;
import com.fab.video_convert_platform.mapper.ProjectConfigMapper;
import com.fab.video_convert_platform.mapper.VideoUploadTaskMapper;
import com.fab.video_convert_platform.service.IArchiveService;
import com.fab.video_convert_platform.service.IVideoService;
import com.fab.video_convert_platform.service.ITaskLogService;
import com.fab.video_convert_platform.infra.NfsService;
import com.fab.video_convert_platform.util.ArchivePathUtil;
import com.fab.video_convert_platform.util.DigestUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    private final ProjectConfigMapper projectConfigMapper;
    private final VideoUploadTaskMapper uploadTaskMapper;
    private final IArchiveService archiveService;
    private final NfsService nfsService;
    private final ITaskLogService taskLogService;
    private final VideoTaskDomainService videoTaskDomainService;

    public VideoServiceImpl(ProjectConfigMapper projectConfigMapper,
                            VideoUploadTaskMapper uploadTaskMapper,
                            IArchiveService archiveService,
                            NfsService nfsService,
                            ITaskLogService taskLogService,
                            VideoTaskDomainService videoTaskDomainService) {
        this.projectConfigMapper = projectConfigMapper;
        this.uploadTaskMapper = uploadTaskMapper;
        this.archiveService = archiveService;
        this.nfsService = nfsService;
        this.taskLogService = taskLogService;
        this.videoTaskDomainService = videoTaskDomainService;
    }

    @Override
    public VideoUploadTask upload(MultipartFile file, String projectNo,
                                  String patientCode, String tpStage) {
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
            task = saveUploadTaskInTransaction(projectNo, patientCode, tpStage,
                uuid, versionNo, fileName, path, file.getSize(), md5);

            taskLogService.info(task.getId(), "original file archived");
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.STORE_FILE_FAILED,
                "Failed to store file: " + e.getMessage());
        }

        // 5. 异步处理视频切片（事务外执行）
        processVideoAsync(config, task);

        return uploadTaskMapper.selectById(task.getId());
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
                // 合并分片
                Path target = ArchivePathUtil.buildOriginalPath(config.getArchiveRoot(),
                        projectNo, patientCode, tpStage, versionNo, uuid, filename);
                nfsService.mergeChunks(chunkDir, target, chunks);

                long size = Files.size(target);
                String md5 = DigestUtil.md5(target);

                // 4. 数据库操作（使用事务）
                VideoUploadTask task = saveUploadTaskInTransaction(projectNo, patientCode,
                    tpStage, uuid, versionNo, filename, target, size, md5);

                taskLogService.info(task.getId(), "chunks merged and archived");

                // 5. 清理分片目录
                nfsService.deleteRecursively(chunkDir);

                // 6. 异步处理视频切片（事务外执行）
                processVideoAsync(config, task);
            }
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.CHUNK_MERGE_FAILED,
                "Failed to process chunk: " + e.getMessage());
        }
    }

    @Override
    public void processMqMessage(MqVideoMessage message) {
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
            VideoUploadTask task = saveUploadTaskInTransaction(message.getProjectNo(),
                    message.getPatientCode(), message.getTpStage(), uuid, versionNo,
                    fileName, target, size, md5);

            taskLogService.info(task.getId(), "mq file archived");

            // 6. 异步处理视频切片（事务外执行）
            processVideoAsync(config, task);

        } catch (IOException e) {
            throw new BusinessException(ErrorCode.MQ_PROCESS_FAILED,
                "Failed to process MQ file: " + e.getMessage());
        }
    }

    /**
     * 验证项目配置
     */
    private ProjectConfig validateProject(String projectNo) {
        ProjectConfig config = projectConfigMapper.selectOne(
                new QueryWrapper<ProjectConfig>().eq("project_no", projectNo).last("limit 1"));
        if (config == null) {
            throw new BusinessException(ErrorCode.PROJECT_NOT_FOUND,
                "Project not found: " + projectNo);
        }
        return config;
    }

    /**
     * 在事务中保存上传任务和归档记录
     */
    @Transactional(rollbackFor = Exception.class)
    public VideoUploadTask saveUploadTaskInTransaction(String projectNo, String patientCode,
            String tpStage, String uuid, Integer versionNo, String fileName,
            Path filePath, Long fileSize, String md5) {

        // 创建上传任务
        VideoUploadTask task = VideoUploadTask.createOriginalSaved(projectNo, patientCode,
                tpStage, uuid, versionNo, VideoConstants.SOURCE_CONTROLLER, fileName,
                filePath.toString(), fileSize, md5);

        uploadTaskMapper.insert(task);

        // 保存归档文件记录
        archiveService.saveOriginal(task.getId(), fileName, filePath.toString(),
                fileSize, md5);

        return task;
    }

    /**
     * 异步处理视频切片（事务外执行）
     */
    private void processVideoAsync(ProjectConfig config, VideoUploadTask task) {
        try {
            // 异步执行视频处理，避免占用事务
            videoTaskDomainService.processSlices(config, task);
        } catch (Exception e) {
            // 处理失败时更新任务状态
            taskLogService.error(task.getId(), "video processing failed: " + e.getMessage());
            task.markError(e.getMessage());
            uploadTaskMapper.updateById(task);

            // 根据异常类型抛出相应的业务异常
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
                throw new BusinessException(ErrorCode.FFMPEG_INTERRUPTED,
                    "Video processing interrupted");
            } else if (e instanceof IOException) {
                throw new BusinessException(ErrorCode.STORE_FILE_FAILED,
                    "Video processing I/O error: " + e.getMessage());
            } else {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                    "Video processing error: " + e.getMessage());
            }
        }
    }

    @Override
    public VideoUploadTask getTaskById(Long taskId) {
        if (taskId == null || taskId <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "任务ID无效");
        }

        VideoUploadTask task = uploadTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ErrorCode.TASK_NOT_FOUND,
                "任务不存在: " + taskId);
        }

        return task;
    }
}
