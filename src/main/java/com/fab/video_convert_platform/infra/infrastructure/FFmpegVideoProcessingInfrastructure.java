package com.fab.video_convert_platform.infra.infrastructure;

import com.fab.video_convert_platform.domain.infrastructure.VideoProcessingInfrastructure;
import com.fab.video_convert_platform.util.FFmpegUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 基于FFmpeg的视频处理基础设施实现
 */
@Slf4j
@Component
public class FFmpegVideoProcessingInfrastructure implements VideoProcessingInfrastructure {

    private final FFmpegUtil ffmpegUtil;

    public FFmpegVideoProcessingInfrastructure(FFmpegUtil ffmpegUtil) {
        this.ffmpegUtil = ffmpegUtil;
    }

    @Override
    public boolean checkVideoIntegrity(Path videoPath) {
        try {
            ffmpegUtil.validate(videoPath);
            return true;
        } catch (Exception e) {
            log.error("Video integrity check failed: path={}, error={}", videoPath, e.getMessage());
            return false;
        }
    }

    @Override
    public int[] getVideoResolution(Path videoPath) {
        try {
            return ffmpegUtil.getResolution(videoPath);
        } catch (Exception e) {
            log.error("Failed to get video resolution: path={}, error={}", videoPath, e.getMessage());
            throw new RuntimeException("Failed to get video resolution", e);
        }
    }

    @Override
    public boolean isAviFormat(Path videoPath) {
        try {
            // 空指针安全检查
            if (videoPath == null || videoPath.getFileName() == null) {
                return false;
            }
            String fileName = videoPath.getFileName().toString().toLowerCase();
            return fileName.endsWith(".avi");
        } catch (Exception e) {
            log.error("Failed to check video format: path={}, error={}", videoPath, e.getMessage());
            return false;
        }
    }

    @Override
    public void convertAviToMp4(Path aviPath, Path mp4Path) {
        try {
            ffmpegUtil.aviToMp4(aviPath, mp4Path);
        } catch (Exception e) {
            log.error("Failed to convert AVI to MP4: aviPath={}, mp4Path={}, error={}", 
                aviPath, mp4Path, e.getMessage());
            throw new RuntimeException("Failed to convert AVI to MP4", e);
        }
    }

    @Override
    public void downscaleVideo(Path inputPath, Path outputPath, int targetWidth, int targetHeight) {
        try {
            ffmpegUtil.transcode(inputPath, outputPath, targetWidth, targetHeight);
        } catch (Exception e) {
            log.error("Failed to downscale video: inputPath={}, outputPath={}, resolution={}x{}, error={}", 
                inputPath, outputPath, targetWidth, targetHeight, e.getMessage());
            throw new RuntimeException("Failed to downscale video", e);
        }
    }

    @Override
    public Path sliceToHls(Path inputPath, Path outputDir, int segmentDuration) {
        try {
            return ffmpegUtil.sliceToM3u8(inputPath, outputDir);
        } catch (Exception e) {
            log.error("Failed to slice video to HLS: inputPath={}, outputDir={}, segmentDuration={}, error={}", 
                inputPath, outputDir, segmentDuration, e.getMessage());
            throw new RuntimeException("Failed to slice video to HLS", e);
        }
    }

    @Override
    public void cleanupTempFiles(List<Path> tempFiles) {
        for (Path tempFile : tempFiles) {
            try {
                if (Files.exists(tempFile)) {
                    Files.deleteIfExists(tempFile);
                    log.debug("Temp file cleaned up: {}", tempFile);
                }
            } catch (IOException e) {
                log.warn("Failed to cleanup temp file: path={}, error={}", tempFile, e.getMessage());
            }
        }
    }
}
