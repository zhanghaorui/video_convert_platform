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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * 视频任务处理领域服务
 * 负责视频转码、切片等核心业务逻辑的编排
 * 遵循DDD设计原则，封装复杂的业务规则
 */
@Slf4j
@Service
public class VideoTaskDomainService {

    private final IArchiveService archiveService;
    private final ITaskLogService taskLogService;
    private final ICallbackService callbackService;
    private final VideoUploadTaskMapper uploadTaskMapper;
    private final FFmpegUtil ffmpegUtil;

    public VideoTaskDomainService(IArchiveService archiveService,
                                  ITaskLogService taskLogService,
                                  ICallbackService callbackService,
                                  VideoUploadTaskMapper uploadTaskMapper,
                                  FFmpegUtil ffmpegUtil) {
        this.archiveService = archiveService;
        this.taskLogService = taskLogService;
        this.callbackService = callbackService;
        this.uploadTaskMapper = uploadTaskMapper;
        this.ffmpegUtil = ffmpegUtil;
    }

    /**
     * 处理视频切片
     * 包含视频验证、格式转换、分辨率调整、多质量切片等完整流程
     *
     * @param config 项目配置
     * @param task   上传任务
     * @throws IOException 文件操作异常
     * @throws InterruptedException FFmpeg处理中断异常
     */
    public void processSlices(ProjectConfig config, VideoUploadTask task)
            throws IOException, InterruptedException {

        log.info("开始处理视频切片: taskId={}, filePath={}", task.getId(), task.getMainFilePath());

        // 标记任务为处理中状态
        task.markProcessing();
        uploadTaskMapper.updateById(task);

        List<Path> tempFiles = new ArrayList<>();

        try {
            taskLogService.info(task.getId(), "开始视频处理流程");

            // 1. 验证视频文件
            Path inputVideo = validateAndGetInputVideo(task);

            // 2. 预处理视频（格式转换、分辨率调整）
            Path processedVideo = preprocessVideo(task, inputVideo, tempFiles);

            // 3. 获取视频分辨率信息
            int[] resolution = getVideoResolution(task, processedVideo);

            // 4. 生成多质量切片
            generateMultiQualitySlices(config, task, processedVideo, resolution);

            // 5. 执行业务回调
            executeCallback(task);

            // 6. 标记任务完成
            task.markFinished();
            uploadTaskMapper.updateById(task);

            taskLogService.info(task.getId(), "视频处理流程完成");
            log.info("视频切片处理完成: taskId={}", task.getId());

        } catch (Exception e) {
            log.error("视频切片处理失败: taskId={}, error={}", task.getId(), e.getMessage(), e);
            taskLogService.error(task.getId(), "视频处理失败: " + e.getMessage());

            task.markError("视频处理失败: " + e.getMessage());
            uploadTaskMapper.updateById(task);

            throw e;
        } finally {
            // 清理临时文件
            cleanupTempFiles(tempFiles);
        }
    }

    /**
     * 验证并获取输入视频文件
     */
    private Path validateAndGetInputVideo(VideoUploadTask task) throws IOException, InterruptedException {
        Path input = Paths.get(task.getMainFilePath());

        if (!Files.exists(input)) {
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND,
                "视频文件不存在: " + task.getMainFilePath());
        }

        taskLogService.info(task.getId(), "开始验证视频文件完整性");
        ffmpegUtil.validate(input);
        taskLogService.info(task.getId(), "视频文件验证通过");

        return input;
    }

    /**
     * 预处理视频：格式转换和分辨率调整
     */
    private Path preprocessVideo(VideoUploadTask task, Path input, List<Path> tempFiles)
            throws IOException, InterruptedException {

        Path processedVideo = input;

        // 1. AVI格式转换
        processedVideo = convertAviIfNeeded(task, processedVideo, tempFiles);

        // 2. 分辨率降级处理
        processedVideo = downscaleIfNeeded(task, processedVideo, tempFiles);

        return processedVideo;
    }

    /**
     * 如果是AVI格式则转换为MP4
     */
    private Path convertAviIfNeeded(VideoUploadTask task, Path input, List<Path> tempFiles)
            throws IOException, InterruptedException {

        String fileName = input.getFileName().toString().toLowerCase();
        if (!fileName.endsWith(".avi")) {
            return input;
        }

        taskLogService.info(task.getId(), "检测到AVI格式，开始转换为MP4");
        Path mp4Path = input.resolveSibling("converted_" + System.currentTimeMillis() + ".mp4");

        ffmpegUtil.aviToMp4(input, mp4Path);
        tempFiles.add(mp4Path);

        taskLogService.info(task.getId(), "AVI转MP4完成");
        return mp4Path;
    }

    /**
     * 如果分辨率过高则降级到1080p
     */
    private Path downscaleIfNeeded(VideoUploadTask task, Path input, List<Path> tempFiles)
            throws IOException, InterruptedException {

        int[] resolution = ffmpegUtil.getResolution(input);
        int width = resolution[0];
        int height = resolution[1];

        if (width <= 1920 && height <= 1080) {
            return input;
        }

        taskLogService.info(task.getId(), "检测到高分辨率视频({}x{})，开始降级到1080p", width, height);
        Path scaledPath = input.resolveSibling("scaled_" + System.currentTimeMillis() + ".mp4");

        ffmpegUtil.transcode(input, scaledPath, 1920, 1080);
        tempFiles.add(scaledPath);

        taskLogService.info(task.getId(), "分辨率降级完成");
        return scaledPath;
    }

    /**
     * 获取视频分辨率信息
     */
    private int[] getVideoResolution(VideoUploadTask task, Path video)
            throws IOException, InterruptedException {

        int[] resolution = ffmpegUtil.getResolution(video);
        if (resolution.length < 2) {
            throw new BusinessException(ErrorCode.VIDEO_RESOLUTION_ERROR,
                "无法获取视频分辨率信息");
        }

        taskLogService.info(task.getId(), "视频分辨率: {}x{}", resolution[0], resolution[1]);
        return resolution;
    }

    /**
     * 生成多质量切片
     */
    private void generateMultiQualitySlices(ProjectConfig config, VideoUploadTask task,
                                          Path input, int[] originalResolution)
            throws IOException, InterruptedException {

        int originalWidth = originalResolution[0];
        int originalHeight = originalResolution[1];

        for (VideoQuality quality : VideoQuality.values()) {
            generateQualitySlice(config, task, input, quality, originalWidth, originalHeight);
        }
    }

    /**
     * 生成指定质量的切片
     */
    private void generateQualitySlice(ProjectConfig config, VideoUploadTask task, Path input,
                                    VideoQuality quality, int originalWidth, int originalHeight)
            throws IOException, InterruptedException {

        String qualityName = quality.getName();
        int targetWidth = quality.getWidth() > 0 ? quality.getWidth() : originalWidth;
        int targetHeight = quality.getHeight() > 0 ? quality.getHeight() : originalHeight;

        taskLogService.info(task.getId(), "开始生成{}质量切片({}x{})", qualityName, targetWidth, targetHeight);

        // 构建切片输出目录
        Path sliceDir = ArchivePathUtil.buildSlicePath(config.getArchiveRoot(),
                task.getProjectNo(), task.getPatientCode(), task.getTpStage(),
                task.getVersionNo(), task.getUuid(), qualityName);

        // 转码到目标分辨率
        Path transcodedPath = sliceDir.resolve("transcoded_" + qualityName + ".mp4");
        ffmpegUtil.transcode(input, transcodedPath, targetWidth, targetHeight);

        try {
            // 切片生成M3U8
            Path m3u8Path = ffmpegUtil.sliceToM3u8(transcodedPath, sliceDir);

            // 保存切片归档记录
            saveSliceArchive(task, qualityName, m3u8Path);

            taskLogService.info(task.getId(), "{}质量切片生成完成", qualityName);
        } finally {
            // 清理转码临时文件
            Files.deleteIfExists(transcodedPath);
        }
    }

    /**
     * 保存切片归档记录
     */
    private void saveSliceArchive(VideoUploadTask task, String quality, Path m3u8Path)
            throws IOException {

        long fileSize = Files.size(m3u8Path);
        String md5 = DigestUtil.md5(m3u8Path);
        String playUrl = ArchivePathUtil.buildPlayUrl(task.getProjectNo(),
                task.getPatientCode(), task.getTpStage(), task.getVersionNo(),
                task.getUuid(), quality);

        archiveService.saveM3u8(task.getId(), quality, VideoConstants.M3U8_NAME,
                m3u8Path.toString(), playUrl, fileSize, md5);
    }

    /**
     * 执行业务回调
     */
    private void executeCallback(VideoUploadTask task) {
        try {
            callbackService.notify(task);
            taskLogService.info(task.getId(), "业务回调执行成功");
        } catch (Exception e) {
            log.warn("业务回调执行失败: taskId={}, error={}", task.getId(), e.getMessage());
            taskLogService.error(task.getId(), "业务回调失败: " + e.getMessage());
            // 回调失败不影响主流程，只记录日志
        }
    }

    /**
     * 清理临时文件
     */
    private void cleanupTempFiles(List<Path> tempFiles) {
        for (Path tempFile : tempFiles) {
            try {
                if (Files.exists(tempFile)) {
                    Files.deleteIfExists(tempFile);
                    log.debug("临时文件清理成功: {}", tempFile);
                }
            } catch (IOException e) {
                log.warn("临时文件清理失败: path={}, error={}", tempFile, e.getMessage());
            }
        }
    }
}
