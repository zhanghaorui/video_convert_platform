package com.example.video_convert_platform.infra.monitor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * 视频处理监控切面
 * 自动收集关键业务操作的性能指标
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class VideoProcessingMonitorAspect {

    private final VideoProcessingMetrics metrics;

    /**
     * 监控视频上传操作
     */
    @Around("execution(* com.example.video_convert_platform.service.IVideoService.upload(..))")
    public Object monitorUpload(ProceedingJoinPoint joinPoint) throws Throwable {
        Instant start = Instant.now();
        Object[] args = joinPoint.getArgs();
        String projectNo = args.length > 1 ? (String) args[1] : "unknown";

        try {
            metrics.recordUpload(projectNo);
            Object result = joinPoint.proceed();

            Duration duration = Duration.between(start, Instant.now());
            metrics.recordUploadDuration(duration, projectNo);

            log.info("视频上传完成: projectNo={}, 耗时={}ms", projectNo, duration.toMillis());
            return result;
        } catch (Exception e) {
            metrics.recordProcessFailure(projectNo, "upload_error");
            log.error("视频上传失败: projectNo={}, error={}", projectNo, e.getMessage());
            throw e;
        }
    }

    /**
     * 监控分片上传操作
     */
    @Around("execution(* com.example.video_convert_platform.service.IVideoService.uploadChunk(..))")
    public Object monitorChunkUpload(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        String projectNo = args.length > 4 ? (String) args[4] : "unknown";

        try {
            metrics.recordChunkUpload(projectNo);
            return joinPoint.proceed();
        } catch (Exception e) {
            metrics.recordProcessFailure(projectNo, "chunk_upload_error");
            throw e;
        }
    }

    /**
     * 监控视频处理流程
     */
    @Around("execution(* com.example.video_convert_platform.domain.service.VideoTaskDomainService.processSlices(..))")
    public Object monitorVideoProcessing(ProceedingJoinPoint joinPoint) throws Throwable {
        Instant start = Instant.now();
        Object[] args = joinPoint.getArgs();

        String projectNo = "unknown";
        Long taskId = null;
        Long fileSize = 0L;

        if (args.length > 1 && args[1] != null) {
            Object task = args[1];
            try {
                projectNo = (String) task.getClass().getMethod("getProjectNo").invoke(task);
                taskId = (Long) task.getClass().getMethod("getId").invoke(task);
                fileSize = (Long) task.getClass().getMethod("getFileSize").invoke(task);
            } catch (Exception e) {
                log.warn("获取任务信息失败: {}", e.getMessage());
            }
        }

        metrics.incrementActiveTasks();

        try {
            Object result = joinPoint.proceed();

            Duration duration = Duration.between(start, Instant.now());
            metrics.recordProcessSuccess(projectNo, fileSize);

            log.info("视频处理完成: taskId={}, projectNo={}, fileSize={}bytes, 耗时={}ms",
                taskId, projectNo, fileSize, duration.toMillis());

            return result;
        } catch (Exception e) {
            String errorType = e.getClass().getSimpleName();
            metrics.recordProcessFailure(projectNo, errorType);

            log.error("视频处理失败: taskId={}, projectNo={}, error={}",
                taskId, projectNo, e.getMessage());
            throw e;
        } finally {
            metrics.decrementActiveTasks();
        }
    }

    /**
     * 监控FFmpeg操作
     */
    @Around("execution(* com.example.video_convert_platform.util.FFmpegUtil.*(..))")
    public Object monitorFFmpegOperations(ProceedingJoinPoint joinPoint) throws Throwable {
        Instant start = Instant.now();
        String methodName = joinPoint.getSignature().getName();

        try {
            Object result = joinPoint.proceed();

            Duration duration = Duration.between(start, Instant.now());
            metrics.recordFFmpegDuration(duration, methodName);

            return result;
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("timeout")) {
                metrics.recordFFmpegTimeout(methodName, "unknown");
            }
            throw e;
        }
    }
}
