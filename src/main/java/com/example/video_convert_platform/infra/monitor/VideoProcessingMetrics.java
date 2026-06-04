package com.example.video_convert_platform.infra.monitor;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 视频处理性能监控指标收集器
 */
@Slf4j
@Component
public class VideoProcessingMetrics {

    @SuppressWarnings("EI_EXPOSE_REP2") // 依赖注入场景，预期行为
    private final MeterRegistry meterRegistry;

    // 计数器
    private final Counter uploadCounter;
    private final Counter chunkUploadCounter;
    private final Counter processSuccessCounter;
    private final Counter processFailureCounter;
    private final Counter ffmpegTimeoutCounter;

    // 计时器
    private final Timer uploadTimer;
    private final Timer ffmpegProcessTimer;
    private final Timer sliceGenerationTimer;

    // 自定义指标
    private final AtomicLong activeTasksCount = new AtomicLong(0);
    private final ConcurrentHashMap<String, AtomicLong> projectMetrics = new ConcurrentHashMap<>();
    private final AtomicLong totalFileSizeProcessed = new AtomicLong(0);

    public VideoProcessingMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        // 初始化计数器
        this.uploadCounter = Counter.builder("video.upload.total")
            .description("Total number of video uploads")
            .register(meterRegistry);

        this.chunkUploadCounter = Counter.builder("video.chunk.upload.total")
            .description("Total number of chunk uploads")
            .register(meterRegistry);

        this.processSuccessCounter = Counter.builder("video.process.success.total")
            .description("Total number of successful video processing")
            .register(meterRegistry);

        this.processFailureCounter = Counter.builder("video.process.failure.total")
            .description("Total number of failed video processing")
            .register(meterRegistry);

        this.ffmpegTimeoutCounter = Counter.builder("video.ffmpeg.timeout.total")
            .description("Total number of FFmpeg timeouts")
            .register(meterRegistry);

        // 初始化计时器
        this.uploadTimer = Timer.builder("video.upload.duration")
            .description("Time taken for video upload")
            .register(meterRegistry);

        this.ffmpegProcessTimer = Timer.builder("video.ffmpeg.duration")
            .description("Time taken for FFmpeg processing")
            .register(meterRegistry);

        this.sliceGenerationTimer = Timer.builder("video.slice.generation.duration")
            .description("Time taken for slice generation")
            .register(meterRegistry);

        // 注册自定义指标
        meterRegistry.gauge("video.active.tasks", activeTasksCount);
        meterRegistry.gauge("video.total.file.size.processed", totalFileSizeProcessed);
    }

    /**
     * 记录视频上传
     */
    public void recordUpload(String projectNo) {
        uploadCounter.increment();
        incrementProjectMetric(projectNo, "upload");
        log.debug("记录视频上传指标: projectNo={}", projectNo);
    }

    /**
     * 记录分片上传
     */
    public void recordChunkUpload(String projectNo) {
        chunkUploadCounter.increment();
        incrementProjectMetric(projectNo, "chunk");
        log.debug("记录分片上传指标: projectNo={}", projectNo);
    }

    /**
     * 记录上传耗时
     */
    public void recordUploadDuration(Duration duration, String projectNo) {
        uploadTimer.record(duration);
        log.debug("记录上传耗时: {}ms, projectNo={}", duration.toMillis(), projectNo);
    }

    /**
     * 记录处理成功
     */
    public void recordProcessSuccess(String projectNo, long fileSize) {
        processSuccessCounter.increment();
        totalFileSizeProcessed.addAndGet(fileSize);
        incrementProjectMetric(projectNo, "success");
        log.info("记录处理成功指标: projectNo={}, fileSize={}bytes", projectNo, fileSize);
    }

    /**
     * 记录处理失败
     */
    public void recordProcessFailure(String projectNo, String errorType) {
        processFailureCounter.increment();
        incrementProjectMetric(projectNo, "failure");

        // 按错误类型分类统计
        Counter.builder("video.process.failure.by.type")
            .tag("error.type", errorType)
            .tag("project", projectNo)
            .description("Video processing failures by error type")
            .register(meterRegistry)
            .increment();

        log.warn("记录处理失败指标: projectNo={}, errorType={}", projectNo, errorType);
    }

    /**
     * 记录FFmpeg处理耗时
     */
    public void recordFFmpegDuration(Duration duration, String operation) {
        Timer.Sample sample = Timer.start(meterRegistry);
        sample.stop(Timer.builder("video.ffmpeg.operation.duration")
            .tag("operation", operation)
            .description("FFmpeg operation duration")
            .register(meterRegistry));

        ffmpegProcessTimer.record(duration);
        log.debug("记录FFmpeg处理耗时: {}ms, operation={}", duration.toMillis(), operation);
    }

    /**
     * 记录FFmpeg超时
     */
    public void recordFFmpegTimeout(String operation, String projectNo) {
        ffmpegTimeoutCounter.increment();

        Counter.builder("video.ffmpeg.timeout.by.operation")
            .tag("operation", operation)
            .tag("project", projectNo)
            .description("FFmpeg timeouts by operation")
            .register(meterRegistry)
            .increment();

        log.error("记录FFmpeg超时: operation={}, projectNo={}", operation, projectNo);
    }

    /**
     * 记录切片生成耗时
     */
    public void recordSliceGenerationDuration(Duration duration, String quality) {
        Timer.builder("video.slice.generation.by.quality")
            .tag("quality", quality)
            .description("Slice generation duration by quality")
            .register(meterRegistry)
            .record(duration);

        sliceGenerationTimer.record(duration);
        log.debug("记录切片生成耗时: {}ms, quality={}", duration.toMillis(), quality);
    }

    /**
     * 增加活跃任务数
     */
    public void incrementActiveTasks() {
        activeTasksCount.incrementAndGet();
        log.debug("活跃任务数增加: {}", activeTasksCount.get());
    }

    /**
     * 减少活跃任务数
     */
    public void decrementActiveTasks() {
        activeTasksCount.decrementAndGet();
        log.debug("活跃任务数减少: {}", activeTasksCount.get());
    }

    /**
     * 获取当前活跃任务数
     */
    public long getActiveTasksCount() {
        return activeTasksCount.get();
    }

    /**
     * 获取项目处理统计
     */
    public long getProjectMetric(String projectNo, String type) {
        String key = projectNo + "." + type;
        return projectMetrics.getOrDefault(key, new AtomicLong(0)).get();
    }

    /**
     * 增加项目指标计数
     */
    private void incrementProjectMetric(String projectNo, String type) {
        String key = projectNo + "." + type;
        projectMetrics.computeIfAbsent(key, k -> {
            AtomicLong counter = new AtomicLong(0);
            meterRegistry.gauge("video.project.metric",
                io.micrometer.core.instrument.Tags.of(
                    "project", projectNo,
                    "type", type),
                counter);
            return counter;
        }).incrementAndGet();
    }
}
