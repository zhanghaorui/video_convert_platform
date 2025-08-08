package com.fab.video_convert_platform.domain.service;

import com.fab.video_convert_platform.common.BusinessException;
import com.fab.video_convert_platform.common.ErrorCode;
import com.fab.video_convert_platform.common.VideoConstants;
import com.fab.video_convert_platform.domain.ProjectConfig;
import com.fab.video_convert_platform.domain.VideoUploadTask;
import com.fab.video_convert_platform.domain.enums.VideoQuality;
import com.fab.video_convert_platform.service.IArchiveService;
import com.fab.video_convert_platform.service.ICallbackService;
import com.fab.video_convert_platform.service.ITaskLogService;
import com.fab.video_convert_platform.mapper.VideoUploadTaskMapper;
import com.fab.video_convert_platform.util.ArchivePathUtil;
import com.fab.video_convert_platform.util.DigestUtil;
import com.fab.video_convert_platform.util.FFmpegUtil;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Domain service containing core rules for video task processing.
 */
@Service
public class VideoTaskDomainService {

    private final IArchiveService archiveService;
    private final ITaskLogService taskLogService;
    private final ICallbackService callbackService;
    private final VideoUploadTaskMapper uploadTaskMapper;

    public VideoTaskDomainService(IArchiveService archiveService,
                                  ITaskLogService taskLogService,
                                  ICallbackService callbackService,
                                  VideoUploadTaskMapper uploadTaskMapper) {
        this.archiveService = archiveService;
        this.taskLogService = taskLogService;
        this.callbackService = callbackService;
        this.uploadTaskMapper = uploadTaskMapper;
    }

    public void processSlices(ProjectConfig config, VideoUploadTask task)
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
        if (res == null || res.length < 2) {
            taskLogService.info(task.getId(), "failed to get video resolution");
            throw BusinessException.of(ErrorCode.VIDEO_RESOLUTION_ERROR,
                    "Failed to parse video resolution for input: " + input);
        }
        int origW = res[0];
        int origH = res[1];
        if (origW > 1920 || origH > 1080) {
            taskLogService.info(task.getId(), "downscale to 1080p");
            Path scaled = input.resolveSibling("tmp_1080p.mp4");
            FFmpegUtil.transcode(input, scaled, 1920, 1080);
            temps.add(scaled);
            input = scaled;
            origW = 1920;
            origH = 1080;
        }
        for (VideoQuality vq : VideoQuality.values()) {
            String quality = vq.getName();
            int w = vq.getWidth() > 0 ? vq.getWidth() : origW;
            int h = vq.getHeight() > 0 ? vq.getHeight() : origH;
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
        try {
            callbackService.notify(task);
            taskLogService.info(task.getId(), "callback finished");
        } catch (Exception e) {
            taskLogService.error(task.getId(), "callback failed: " + e.getMessage());
        }
        task.markFinished();
        uploadTaskMapper.updateById(task);
        taskLogService.info(task.getId(), "task finished");
    }
}
