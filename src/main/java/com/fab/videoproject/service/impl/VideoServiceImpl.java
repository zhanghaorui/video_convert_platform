package com.fab.videoproject.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fab.videoproject.common.BusinessException;
import com.fab.videoproject.common.VideoConstants;
import com.fab.videoproject.domain.ProjectConfig;
import com.fab.videoproject.domain.VideoUploadTask;
import com.fab.videoproject.domain.enums.TaskStatus;
import com.fab.videoproject.infra.MqVideoMessage;
import com.fab.videoproject.mapper.ProjectConfigMapper;
import com.fab.videoproject.mapper.VideoUploadTaskMapper;
import com.fab.videoproject.service.IArchiveService;
import com.fab.videoproject.service.IVideoService;
import com.fab.videoproject.service.ITaskLogService;
import com.fab.videoproject.service.ICallbackService;
import com.fab.videoproject.infra.NfsService;
import com.fab.videoproject.util.ArchivePathUtil;
import com.fab.videoproject.util.DigestUtil;
import com.fab.videoproject.util.FFmpegUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Implementation of video service operations.
 */
@Service
public class VideoServiceImpl implements IVideoService {

    @Autowired
    private ProjectConfigMapper projectConfigMapper;
    @Autowired
    private VideoUploadTaskMapper uploadTaskMapper;
    @Autowired
    private IArchiveService archiveService;
    @Autowired
    private NfsService nfsService;
    @Autowired
    private ITaskLogService taskLogService;
    @Autowired
    private ICallbackService callbackService;

    @Override
    @Transactional(rollbackFor = RuntimeException.class)
    public VideoUploadTask upload(MultipartFile file, String projectNo,
                                  String patientCode, String tpStage) {
        ProjectConfig config = projectConfigMapper.selectOne(
                new QueryWrapper<ProjectConfig>().eq("project_no", projectNo).last("limit 1"));
        if (config == null) {
            throw BusinessException.error("Project config not found");
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
            processSlices(config, task);
            return uploadTaskMapper.selectById(task.getId());
        } catch (IOException e) {
            if (task != null) {
                taskLogService.error(task.getId(), "store file failed: " + e.getMessage());
            }
            throw BusinessException.error("Failed to store file: " + e.getMessage());
        } catch (InterruptedException e) {
            if (task != null) {
                taskLogService.error(task.getId(), "ffmpeg interrupted");
            }
            Thread.currentThread().interrupt();
            throw BusinessException.error("FFmpeg interrupted");
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
            throw BusinessException.error("Project config not found");
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
                processSlices(config, task);
                nfsService.deleteRecursively(chunkDir);
            }
        } catch (IOException e) {
            if (task != null) {
                taskLogService.error(task.getId(), "chunk process failed: " + e.getMessage());
            }
            throw BusinessException.error("Failed to store chunk: " + e.getMessage());
        } catch (InterruptedException e) {
            if (task != null) {
                taskLogService.error(task.getId(), "ffmpeg interrupted");
            }
            Thread.currentThread().interrupt();
            throw BusinessException.error("FFmpeg interrupted");
        }
    }

    @Override
    @Transactional(rollbackFor = RuntimeException.class)
    public void processMqMessage(MqVideoMessage message) {
        ProjectConfig config = projectConfigMapper.selectOne(
                new QueryWrapper<ProjectConfig>().eq("project_no", message.getProjectNo()).last("limit 1"));
        if (config == null) {
            throw BusinessException.error("Project config not found");
        }
        Path source = Paths.get(message.getFilePath());
        if (!Files.exists(source)) {
            throw BusinessException.error("Source file not found");
        }
        VideoUploadTask task = null;
        try {
            if (message.getFileMd5() == null) {
                throw BusinessException.error("MD5 is required for MQ message");
            }
            String md5 = DigestUtil.md5(source);
            if (!message.getFileMd5().equalsIgnoreCase(md5)) {
                throw BusinessException.error("MD5 mismatch");
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
            processSlices(config, task);
        } catch (IOException e) {
            if (task != null) {
                taskLogService.error(task.getId(), "mq process failed: " + e.getMessage());
            }
            throw BusinessException.error("Failed to process MQ file: " + e.getMessage());
        } catch (InterruptedException e) {
            if (task != null) {
                taskLogService.error(task.getId(), "ffmpeg interrupted");
            }
            Thread.currentThread().interrupt();
            throw BusinessException.error("FFmpeg interrupted");
        }
    }

    private void processSlices(ProjectConfig config, VideoUploadTask task)
            throws IOException, InterruptedException {
        taskLogService.info(task.getId(), "start slicing");
        Path input = Paths.get(task.getMainFilePath());
        taskLogService.info(task.getId(), "validate video");
        FFmpegUtil.validate(input);
        List<Path> temps = new ArrayList<>();
        String name = input.getFileName().toString().toLowerCase();
        if (name.endsWith(".avi")) {
            taskLogService.info(task.getId(), "convert avi to mp4");
            Path mp4 = input.resolveSibling("converted.mp4");
            FFmpegUtil.aviToMp4(input, mp4);
            temps.add(mp4);
            input = mp4;
        }
        int[] res = FFmpegUtil.getResolution(input);
        if (res[0] > 1920 || res[1] > 1080) {
            taskLogService.info(task.getId(), "downscale to 1080p");
            Path scaled = input.resolveSibling("tmp_1080p.mp4");
            FFmpegUtil.transcode(input, scaled, 1920, 1080);
            temps.add(scaled);
            input = scaled;
        }
        String[] qualities = {VideoConstants.QUALITY_LOW, VideoConstants.QUALITY_NORMAL};
        int[][] scales = {{640, 360}, {1280, 720}};
        for (int i = 0; i < qualities.length; i++) {
            String quality = qualities[i];
            int w = scales[i][0];
            int h = scales[i][1];
            Path sliceDir = ArchivePathUtil.buildSlicePath(config.getArchiveRoot(),
                    task.getProjectNo(), task.getPatientCode(), task.getTpStage(),
                    task.getVersionNo(), task.getUuid(), quality);
            Path transcoded = sliceDir.resolve("tmp_" + quality + ".mp4");
            taskLogService.info(task.getId(), "transcoding " + quality);
            FFmpegUtil.transcode(input, transcoded, w, h);
            Path m3u8 = FFmpegUtil.sliceToM3u8(transcoded, sliceDir);
            Files.deleteIfExists(transcoded);
            long size = Files.size(m3u8);
            String md5 = DigestUtil.md5(m3u8);
            String playUrl = ArchivePathUtil.buildPlayUrl(task.getProjectNo(),
                    task.getPatientCode(), task.getTpStage(), task.getVersionNo(),
                    task.getUuid(), quality);
            archiveService.saveM3u8(task.getId(), quality, VideoConstants.M3U8_NAME,
                    m3u8.toString(), playUrl, size, md5);
            taskLogService.info(task.getId(), "slice " + quality + " ready");
        }
        for (Path tmp : temps) {
            Files.deleteIfExists(tmp);
        }
        callbackService.notify(task);
        taskLogService.info(task.getId(), "callback finished");
        task.setStatus(TaskStatus.FINISHED.name());
        uploadTaskMapper.updateById(task);
        taskLogService.info(task.getId(), "task finished");
    }
}
