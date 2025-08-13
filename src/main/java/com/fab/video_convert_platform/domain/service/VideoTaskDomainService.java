package com.fab.video_convert_platform.domain.service;

import com.fab.video_convert_platform.common.BusinessException;
import com.fab.video_convert_platform.common.ErrorCode;
import com.fab.video_convert_platform.common.VideoConstants;
import com.fab.video_convert_platform.config.NfsProperties;
import com.fab.video_convert_platform.domain.ProjectConfig;
import com.fab.video_convert_platform.domain.VideoUploadTask;
import com.fab.video_convert_platform.domain.enums.VideoQuality;
import com.fab.video_convert_platform.domain.event.DomainEventPublisher;
import com.fab.video_convert_platform.domain.event.SliceGeneratedEvent;
import com.fab.video_convert_platform.domain.event.TaskCallbackEvent;
import com.fab.video_convert_platform.domain.event.TaskLogEvent;
import com.fab.video_convert_platform.domain.repository.VideoUploadTaskRepository;
import com.fab.video_convert_platform.util.ArchivePathUtil;
import com.fab.video_convert_platform.util.DigestUtil;
import com.fab.video_convert_platform.util.FFmpegUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 视频任务处理领域服务
 * 负责视频转码、切片等核心业务逻辑的编排
 * 遵循DDD设计原则，封装复杂的业务规则
 * @author 张浩锐
 */
@Slf4j
@Service
public class VideoTaskDomainService {

    @SuppressWarnings("EI_EXPOSE_REP2") // 依赖注入场景，预期行为
    private final VideoUploadTaskRepository uploadTaskRepository;
    @SuppressWarnings("EI_EXPOSE_REP2") // 依赖注入场景，预期行为
    private final FFmpegUtil ffmpegUtil;
    @SuppressWarnings("EI_EXPOSE_REP2") // 依赖注入场景，预期行为
    private final Tracer tracer;
    @SuppressWarnings("EI_EXPOSE_REP2") // 依赖注入场景，预期行为
    private final DomainEventPublisher eventPublisher;
    @SuppressWarnings("EI_EXPOSE_REP2") // 依赖注入场景，预期行为
    private final NfsProperties nfsProperties;

    public VideoTaskDomainService(VideoUploadTaskRepository uploadTaskRepository,
                                  FFmpegUtil ffmpegUtil,
                                  Tracer tracer,
                                  DomainEventPublisher eventPublisher,
                                  NfsProperties nfsProperties) {
        this.uploadTaskRepository = uploadTaskRepository;
        this.ffmpegUtil = ffmpegUtil;
        this.tracer = tracer;
        this.eventPublisher = eventPublisher;
        this.nfsProperties = nfsProperties;
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
        uploadTaskRepository.save(task);

        List<Path> tempFiles = new ArrayList<>();

        try {
            eventPublisher.publish(new TaskLogEvent(task.getId(), TaskLogEvent.Level.INFO,
                "开始视频处理流程"));

            // 1. 验证视频文件
            Path inputVideo = validateAndGetInputVideo(task);

            // 2. 预处理视频（格式转换、分辨率调整）
            Path processedVideo = preprocessVideo(task, inputVideo, tempFiles);

            // 3. 获取视频分辨率信息
            int[] resolution = getVideoResolution(task, processedVideo);

            // 4. 生成多质量切片
            generateMultiQualitySlices(config, task, processedVideo, resolution);

            // 5. 标记任务完成
            task.markFinished();
            uploadTaskRepository.save(task);

            // 6. 发布业务回调事件
            eventPublisher.publish(new TaskCallbackEvent(task));

            eventPublisher.publish(new TaskLogEvent(task.getId(), TaskLogEvent.Level.INFO,
                "视频处理流程完成"));
            log.info("视频切片处理完成: taskId={}", task.getId());

        } catch (IOException | InterruptedException | BusinessException e) {
            log.error("视频切片处理失败: taskId={}, error={}", task.getId(), e.getMessage(), e);
            eventPublisher.publish(new TaskLogEvent(task.getId(), TaskLogEvent.Level.ERROR,
                "视频处理失败: " + e.getMessage()));

            // 标记任务失败
            task.markError("视频处理失败: " + e.getMessage());
            uploadTaskRepository.save(task);

            // 发布失败回调事件
            eventPublisher.publish(new TaskCallbackEvent(task));

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

        eventPublisher.publish(new TaskLogEvent(task.getId(), TaskLogEvent.Level.INFO,
            "开始验证视频文件完整性"));
        ffmpegUtil.validate(input);
        eventPublisher.publish(new TaskLogEvent(task.getId(), TaskLogEvent.Level.INFO,
            "视频文件验证通过"));

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

        // 空指针安全检查
        if (input == null) {
            throw new IllegalArgumentException("输入文件路径不能为空");
        }
        Path fileNamePath = input.getFileName();
        if (fileNamePath == null) {
            throw new IllegalArgumentException("输入文件路径不能为空");
        }

        String fileName = fileNamePath.toString().toLowerCase();
        if (!fileName.endsWith(".avi")) {
            return input;
        }

        eventPublisher.publish(new TaskLogEvent(task.getId(), TaskLogEvent.Level.INFO,
            "检测到AVI格式，开始转换为MP4"));
        Path mp4Path = input.resolveSibling("converted_" + System.currentTimeMillis() + ".mp4");

        ffmpegUtil.aviToMp4(input, mp4Path);
        tempFiles.add(mp4Path);

        eventPublisher.publish(new TaskLogEvent(task.getId(), TaskLogEvent.Level.INFO,
            "AVI转MP4完成"));
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

        eventPublisher.publish(new TaskLogEvent(task.getId(), TaskLogEvent.Level.INFO,
            String.format("检测到高分辨率视频(%dx%d)，开始降级到1080p", width, height)));
        Path scaledPath = input.resolveSibling("scaled_" + System.currentTimeMillis() + ".mp4");

        ffmpegUtil.transcode(input, scaledPath, 1920, 1080);
        tempFiles.add(scaledPath);

        eventPublisher.publish(new TaskLogEvent(task.getId(), TaskLogEvent.Level.INFO,
            "分辨率降级完成"));
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

        eventPublisher.publish(new TaskLogEvent(task.getId(), TaskLogEvent.Level.INFO,
            String.format("视频分辨率: %dx%d", resolution[0], resolution[1])));
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

        Span span = tracer.nextSpan().name("ffmpeg_" + qualityName).start();
        span.tag("task_id", String.valueOf(task.getId()));
        span.tag("quality", qualityName);
        try (Tracer.SpanInScope ws = tracer.withSpan(span)) {
            eventPublisher.publish(new TaskLogEvent(task.getId(), TaskLogEvent.Level.INFO,
                String.format("开始生成%s质量切片(%dx%d)", qualityName, targetWidth, targetHeight)));

            // 构建切片输出目录
            Path sliceDir = ArchivePathUtil.buildSlicePath(config.getArchiveRoot(),
                    task.getProjectNo(), task.getPatientCode(), task.getTpStage(),
                    task.getVersionNo(), task.getUuid(), qualityName);

            // 转码到目标分辨率
            Path transcodedPath = sliceDir.resolve("transcoded_" + qualityName + ".mp4");
            try {
                ffmpegUtil.transcode(input, transcodedPath, targetWidth, targetHeight);
                // 切片生成M3U8
                Path m3u8Path = ffmpegUtil.sliceToM3u8(transcodedPath, sliceDir);

                // 发布切片生成事件
                saveSliceArchive(task, qualityName, m3u8Path);

                span.tag("exit_code", "0");
                eventPublisher.publish(new TaskLogEvent(task.getId(), TaskLogEvent.Level.INFO,
                    qualityName + "质量切片生成完成"));
            } catch (BusinessException e) {
                span.tag("exit_code", extractExitCode(e.getMessage()));
                span.error(e);
                throw e;
            } finally {
                // 清理转码临时文件
                Files.deleteIfExists(transcodedPath);
            }
        } finally {
            span.end();
        }
    }

    /**
     * 保存切片归档信息
     */
    private void saveSliceArchive(VideoUploadTask task, String quality, Path m3u8Path)
            throws IOException {

        long fileSize = Files.size(m3u8Path);
        String md5 = DigestUtil.md5(m3u8Path);
        
        // 根据配置决定存储相对路径还是完整URL
        String playUrl;
        if (nfsProperties.getUrlStorageStrategy() == NfsProperties.UrlStorageStrategy.ABSOLUTE) {
            // 存储完整URL
            String relativePath = ArchivePathUtil.buildPlayUrl(task.getProjectNo(),
                    task.getPatientCode(), task.getTpStage(), task.getVersionNo(),
                    task.getUuid(), quality);
            playUrl = buildAbsoluteUrl(relativePath);
        } else {
            // 存储相对路径（默认）
            playUrl = ArchivePathUtil.buildPlayUrl(task.getProjectNo(),
                    task.getPatientCode(), task.getTpStage(), task.getVersionNo(),
                    task.getUuid(), quality);
        }

        eventPublisher.publish(new SliceGeneratedEvent(task.getId(), quality,
                VideoConstants.M3U8_NAME, m3u8Path.toString(), playUrl, fileSize, md5));
    }
    
    /**
     * 构建完整URL
     */
    private String buildAbsoluteUrl(String relativePath) {
        String baseUrl = nfsProperties.getBaseUrl();
        if (!StringUtils.hasText(baseUrl)) {
            return relativePath;
        }
        
        String cleanBaseUrl = baseUrl.replaceAll("/$", "");
        String cleanRelativePath = relativePath.startsWith("/") ? relativePath : "/" + relativePath;
        return cleanBaseUrl + cleanRelativePath;
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

    private String extractExitCode(String message) {
        Matcher m = Pattern.compile("exit code (\\d+)").matcher(message);
        return m.find() ? m.group(1) : "-1";
    }
}
