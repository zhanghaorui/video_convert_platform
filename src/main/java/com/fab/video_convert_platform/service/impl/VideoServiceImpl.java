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
import com.fab.video_convert_platform.service.ICallbackService;
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
 * Implementation of video service operations.
 */
@Service
public class VideoServiceImpl implements IVideoService {

    private final ProjectConfigMapper projectConfigMapper;
    private final VideoUploadTaskMapper uploadTaskMapper;
    private final IArchiveService archiveService;
    private final NfsService nfsService;
    private final ITaskLogService taskLogService;
    private final ICallbackService callbackService;
    private final VideoTaskDomainService videoTaskDomainService;

    public VideoServiceImpl(ProjectConfigMapper projectConfigMapper,
                            VideoUploadTaskMapper uploadTaskMapper,
                            IArchiveService archiveService,
                            NfsService nfsService,
                            ITaskLogService taskLogService,
                            ICallbackService callbackService,
                            VideoTaskDomainService videoTaskDomainService) {
        this.projectConfigMapper = projectConfigMapper;
        this.uploadTaskMapper = uploadTaskMapper;
        this.archiveService = archiveService;
        this.nfsService = nfsService;
        this.taskLogService = taskLogService;
        this.callbackService = callbackService;
        this.videoTaskDomainService = videoTaskDomainService;
    }

    @Override
    @Transactional(rollbackFor = RuntimeException.class)
    public VideoUploadTask upload(MultipartFile file, String projectNo,
                                  String patientCode, String tpStage) {
        ProjectConfig config = projectConfigMapper.selectOne(
                new QueryWrapper<ProjectConfig>().eq("project_no", projectNo).last("limit 1"));
        if (config == null) {
            throw BusinessException.of(ErrorCode.PROJECT_NOT_FOUND);
        }

        String uuid = UUID.randomUUID().toString().replace("-", "");
        int versionNo = VideoConstants.DEFAULT_VERSION_NO;
        String fileName = file.getOriginalFilename();
        Path path = ArchivePathUtil.buildOriginalPath(config.getArchiveRoot(),
                projectNo, patientCode, tpStage, versionNo, uuid, fileName);
        VideoUploadTask task = null;
        try {
            nfsService.saveFile(file, path);
            String md5 = DigestUtil.md5(path);
            task = VideoUploadTask.createOriginalSaved(projectNo, patientCode,
                    tpStage, uuid, versionNo, VideoConstants.SOURCE_CONTROLLER, fileName,
                    path.toString(), file.getSize(), md5);
            uploadTaskMapper.insert(task);
            archiveService.saveOriginal(task.getId(), fileName, path.toString(),
                    file.getSize(), md5);
            taskLogService.info(task.getId(), "original file archived");
            videoTaskDomainService.processSlices(config, task);
            return uploadTaskMapper.selectById(task.getId());
        } catch (IOException e) {
            if (task != null) {
                taskLogService.error(task.getId(), "store file failed: " + e.getMessage());
                task.markError(e.getMessage());
                uploadTaskMapper.updateById(task);
            }
            throw BusinessException.of(ErrorCode.STORE_FILE_FAILED, "Failed to store file: " + e.getMessage());
        } catch (InterruptedException e) {
            if (task != null) {
                taskLogService.error(task.getId(), "ffmpeg interrupted");
                task.markError("ffmpeg interrupted");
                uploadTaskMapper.updateById(task);
            }
            Thread.currentThread().interrupt();
            throw BusinessException.of(ErrorCode.FFMPEG_INTERRUPTED);
        }
    }

    @Override
    @Transactional(rollbackFor = RuntimeException.class)
    public void uploadChunk(MultipartFile file, Integer chunk, Integer chunks,
                            String filename, String projectNo, String patientCode,
                            String tpStage, String uuid) {
        ProjectConfig config = projectConfigMapper.selectOne(
                new QueryWrapper<ProjectConfig>().eq("project_no", projectNo).last("limit 1"));
        if (config == null) {
            throw BusinessException.of(ErrorCode.PROJECT_NOT_FOUND);
        }

        int versionNo = VideoConstants.DEFAULT_VERSION_NO;
        Path chunkDir = ArchivePathUtil.buildChunkPath(config.getArchiveRoot(),
                projectNo, patientCode, tpStage, versionNo, uuid);
        VideoUploadTask task = null;
        try {
            nfsService.saveChunk(file, chunkDir, chunk == null ? 0 : chunk);
            if (chunk != null && chunks != null && chunk + 1 == chunks) {
                Path target = ArchivePathUtil.buildOriginalPath(config.getArchiveRoot(),
                        projectNo, patientCode, tpStage, versionNo, uuid, filename);
                nfsService.mergeChunks(chunkDir, target, chunks);
                long size = Files.size(target);
                String md5 = DigestUtil.md5(target);
                task = VideoUploadTask.createOriginalSaved(projectNo, patientCode,
                        tpStage, uuid, versionNo, VideoConstants.SOURCE_CONTROLLER, filename,
                        target.toString(), size, md5);
                uploadTaskMapper.insert(task);
                archiveService.saveOriginal(task.getId(), filename, target.toString(),
                        size, md5);
                taskLogService.info(task.getId(), "chunks merged and archived");
                videoTaskDomainService.processSlices(config, task);
                nfsService.deleteRecursively(chunkDir);
            }
        } catch (IOException e) {
            if (task != null) {
                taskLogService.error(task.getId(), "chunk process failed: " + e.getMessage());
                task.markError(e.getMessage());
                uploadTaskMapper.updateById(task);
            }
            throw BusinessException.of(ErrorCode.STORE_FILE_FAILED, "Failed to store chunk: " + e.getMessage());
        } catch (InterruptedException e) {
            if (task != null) {
                taskLogService.error(task.getId(), "ffmpeg interrupted");
                task.markError("ffmpeg interrupted");
                uploadTaskMapper.updateById(task);
            }
            Thread.currentThread().interrupt();
            throw BusinessException.of(ErrorCode.FFMPEG_INTERRUPTED);
        }
    }

    @Override
    @Transactional(rollbackFor = RuntimeException.class)
    public void processMqMessage(MqVideoMessage message) {
        ProjectConfig config = projectConfigMapper.selectOne(
                new QueryWrapper<ProjectConfig>().eq("project_no", message.getProjectNo()).last("limit 1"));
        if (config == null) {
            throw BusinessException.of(ErrorCode.PROJECT_NOT_FOUND);
        }
        Path source = Paths.get(message.getFilePath());
        if (!Files.exists(source)) {
            throw BusinessException.of(ErrorCode.SOURCE_FILE_NOT_FOUND);
        }
        VideoUploadTask task = null;
        try {
            if (message.getFileMd5() == null) {
                throw BusinessException.of(ErrorCode.MD5_REQUIRED);
            }
            String md5 = DigestUtil.md5(source);
            if (!message.getFileMd5().equalsIgnoreCase(md5)) {
                throw BusinessException.of(ErrorCode.MD5_MISMATCH);
            }
            String fileName = source.getFileName().toString();
            int versionNo = VideoConstants.DEFAULT_VERSION_NO;
            String uuid = UUID.randomUUID().toString().replace("-", "");
            Path target = ArchivePathUtil.buildOriginalPath(config.getArchiveRoot(),
                    message.getProjectNo(), message.getPatientCode(), message.getTpStage(),
                    versionNo, uuid, fileName);
            nfsService.copyFile(source, target);
            long size = Files.size(target);
            task = VideoUploadTask.createOriginalSaved(message.getProjectNo(),
                    message.getPatientCode(), message.getTpStage(), uuid, versionNo,
                    VideoConstants.SOURCE_MQ, fileName, target.toString(), size, md5);
            uploadTaskMapper.insert(task);
            archiveService.saveOriginal(task.getId(), fileName, target.toString(), size, md5);
            taskLogService.info(task.getId(), "mq file archived");
            videoTaskDomainService.processSlices(config, task);
        } catch (IOException e) {
            if (task != null) {
                taskLogService.error(task.getId(), "mq process failed: " + e.getMessage());
                task.markError(e.getMessage());
                uploadTaskMapper.updateById(task);
            }
            throw BusinessException.of(ErrorCode.MQ_PROCESS_FAILED, "Failed to process MQ file: " + e.getMessage());
        } catch (InterruptedException e) {
            if (task != null) {
                taskLogService.error(task.getId(), "ffmpeg interrupted");
                task.markError("ffmpeg interrupted");
                uploadTaskMapper.updateById(task);
            }
            Thread.currentThread().interrupt();
            throw BusinessException.of(ErrorCode.FFMPEG_INTERRUPTED);
        }
    }

}
