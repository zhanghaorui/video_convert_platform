package com.fab.video_convert_platform.util;

import com.fab.video_convert_platform.common.BusinessException;
import com.fab.video_convert_platform.common.ErrorCode;
import com.fab.video_convert_platform.config.VideoProcessingProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Helper utilities for invoking FFmpeg command line with timeout support.
 */
@Slf4j
@Component
@SuppressWarnings("EI_EXPOSE_REP2") // 依赖注入场景，预期行为
public class FFmpegUtil {

    @SuppressWarnings("EI_EXPOSE_REP2") // 依赖注入场景，预期行为
    private final VideoProcessingProperties properties;

    public FFmpegUtil(VideoProcessingProperties properties) {
        this.properties = properties;
    }

    /**
     * Execute given command with timeout control.
     */
    public void runCommand(List<String> command) throws IOException, InterruptedException {
        runCommand(command, properties.getFfmpeg().getTimeout());
    }

    /**
     * Execute given command with custom timeout.
     */
    public void runCommand(List<String> command, long timeoutMs) throws IOException, InterruptedException {
        log.info("执行FFmpeg命令: {}", String.join(" ", command));

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        boolean finished = false;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {

            // 使用超时等待进程完成
            finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);

            if (!finished) {
                log.warn("FFmpeg命令执行超时，正在强制终止进程");
                process.destroyForcibly();
                throw new BusinessException(ErrorCode.FFMPEG_TIMEOUT,
                        "FFmpeg processing timeout after " + timeoutMs + "ms");
            }

            // 读取输出日志
            String line;
            while ((line = reader.readLine()) != null) {
                log.debug("FFmpeg输出: {}", line);
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                log.error("FFmpeg命令执行失败，退出码: {}", exitCode);
                throw new BusinessException(ErrorCode.FFMPEG_COMMAND_FAILED,
                        "FFmpeg command failed with exit code " + exitCode);
            }

            log.info("FFmpeg命令执行成功");
        } finally {
            if (!finished && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    /**
     * Validate video file integrity by invoking ffmpeg with error reporting.
     */
    public void validate(Path input) throws IOException, InterruptedException {
        log.info("开始验证视频文件完整性: {}", input);

        if (!Files.exists(input)) {
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND,
                    "Video file not found: " + input);
        }

        List<String> cmd = new ArrayList<>();
        cmd.add(properties.getFfmpeg().getExecutablePath());
        cmd.add("-v");
        cmd.add("error");
        cmd.add("-i");
        cmd.add(input.toString());
        cmd.add("-f");
        cmd.add("null");
        cmd.add("-");

        try {
            runCommand(cmd);
            log.info("视频文件验证通过: {}", input);
        } catch (BusinessException e) {
            if (e.getErrorCode() == ErrorCode.FFMPEG_COMMAND_FAILED) {
                throw new BusinessException(ErrorCode.VIDEO_CORRUPTED,
                        "Video file is corrupted: " + input);
            }
            throw e;
        }
    }

    /**
     * Convert AVI video to MP4 using H.264/AAC codecs.
     */
    public void aviToMp4(Path input, Path output) throws IOException, InterruptedException {
        // 输入参数验证
        if (input == null) {
            throw new IllegalArgumentException("输入文件路径不能为空");
        }
        if (output == null) {
            throw new IllegalArgumentException("输出文件路径不能为空");
        }
        
        log.info("开始转换AVI到MP4: {} -> {}", input, output);

        // 空指针安全检查
        Path parent = output.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        List<String> cmd = new ArrayList<>();
        cmd.add(properties.getFfmpeg().getExecutablePath());
        if (properties.getFfmpeg().isUseVideoToolbox()) {
            cmd.add("-hwaccel");
            cmd.add("videotoolbox");
        }
        cmd.add("-i");
        cmd.add(input.toString());
        cmd.add("-c:v");
        if (properties.getFfmpeg().isUseVideoToolbox()) {
            cmd.add("h264_videotoolbox");
        } else {
            cmd.add("libx264");
        }
        cmd.add("-c:a");
        cmd.add("aac");
        cmd.add("-threads");
        cmd.add(String.valueOf(properties.getFfmpeg().getThreads()));
        cmd.add(output.toString());

        try {
            runCommand(cmd);
            log.info("AVI转MP4完成: {}", output);
        } catch (BusinessException e) {
            if (e.getErrorCode() == ErrorCode.FFMPEG_COMMAND_FAILED) {
                throw new BusinessException(ErrorCode.TRANSCODE_FAILED,
                        "Failed to convert AVI to MP4: " + e.getMessage());
            }
            throw e;
        }
    }

    /**
     * Probe video resolution using ffprobe with timeout.
     */
    public int[] getResolution(Path input) throws IOException, InterruptedException {
        log.info("获取视频分辨率: {}", input);

        List<String> cmd = new ArrayList<>();
        cmd.add("ffprobe");
        cmd.add("-v");
        cmd.add("error");
        cmd.add("-select_streams");
        cmd.add("v:0");
        cmd.add("-show_entries");
        cmd.add("stream=width,height");
        cmd.add("-of");
        cmd.add("csv=s=x:p=0");
        cmd.add(input.toString());

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        String result;
        boolean finished = false;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             InputStream in = process.getInputStream()) {

            // 使用较短的超时时间获取分辨率
            finished = process.waitFor(10000, TimeUnit.MILLISECONDS);

            if (!finished) {
                process.destroyForcibly();
                throw new BusinessException(ErrorCode.FFMPEG_TIMEOUT,
                        "FFprobe timeout when getting resolution");
            }

            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) != -1) {
                baos.write(buf, 0, len);
            }
            result = baos.toString(StandardCharsets.UTF_8.name()).trim();
        } finally {
            if (!finished && process.isAlive()) {
                process.destroyForcibly();
            }
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            throw new BusinessException(ErrorCode.VIDEO_RESOLUTION_ERROR,
                    "Failed to get video resolution, ffprobe exit code: " + exitCode);
        }

        String[] parts = result.split("x");
        if (parts.length != 2) {
            throw new BusinessException(ErrorCode.VIDEO_RESOLUTION_ERROR,
                    "Unexpected ffprobe output format: '" + result + "'");
        }

        int w, h;
        try {
            w = Integer.parseInt(parts[0].trim());
            h = Integer.parseInt(parts[1].trim());
        } catch (NumberFormatException e) {
            log.error("解析视频分辨率失败: {}", e.getMessage());
            throw new BusinessException(ErrorCode.VIDEO_RESOLUTION_ERROR,
                    "Malformed ffprobe output: non-numeric resolution '" + result + "'. Error: " + e.getMessage());
        }

        log.info("视频分辨率: {}x{}", w, h);
        return new int[]{w, h};
    }

    /**
     * Transcode input video into mp4 with specified scale.
     */
    public void transcode(Path input, Path output, int width, int height)
            throws IOException, InterruptedException {
        // 输入参数验证
        if (input == null) {
            throw new IllegalArgumentException("输入文件路径不能为空");
        }
        if (output == null) {
            throw new IllegalArgumentException("输出文件路径不能为空");
        }
        
        log.info("开始转码视频: {} -> {} ({}x{})", input, output, width, height);

        // 空指针安全检查
        Path parent = output.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        List<String> cmd = new ArrayList<>();
        cmd.add(properties.getFfmpeg().getExecutablePath());
        if (properties.getFfmpeg().isUseVideoToolbox()) {
            cmd.add("-hwaccel");
            cmd.add("videotoolbox");
        }
        cmd.add("-i");
        cmd.add(input.toString());
        cmd.add("-vf");
        cmd.add("scale=" + width + ":" + height);
        cmd.add("-c:v");
        if (properties.getFfmpeg().isUseVideoToolbox()) {
            cmd.add("h264_videotoolbox");
            cmd.add("-b:v");
            cmd.add("4000k");
        } else {
            cmd.add("libx264");
            cmd.add("-preset");
            cmd.add("medium");
            cmd.add("-crf");
            cmd.add("23");
        }
        cmd.add("-threads");
        cmd.add(String.valueOf(properties.getFfmpeg().getThreads()));
        cmd.add(output.toString());

        try {
            runCommand(cmd);
            log.info("视频转码完成: {}", output);
        } catch (BusinessException e) {
            if (e.getErrorCode() == ErrorCode.FFMPEG_COMMAND_FAILED ||
                    e.getErrorCode() == ErrorCode.FFMPEG_TIMEOUT) {
                throw new BusinessException(ErrorCode.TRANSCODE_FAILED,
                        "Failed to transcode video: " + e.getMessage());
            }
            throw e;
        }
    }

    /**
     * Slice mp4 file into HLS m3u8 under target directory.
     */
    public Path sliceToM3u8(Path input, Path outputDir) throws IOException, InterruptedException {
        log.info("开始切片生成M3U8: {} -> {}", input, outputDir);

        Files.createDirectories(outputDir);
        Path m3u8Path = outputDir.resolve("index.m3u8");

        List<String> cmd = new ArrayList<>();
        cmd.add(properties.getFfmpeg().getExecutablePath());
        cmd.add("-i");
        cmd.add(input.toString());
        cmd.add("-c:v");
        cmd.add("copy");
        cmd.add("-c:a");
        cmd.add("copy");
        cmd.add("-f");
        cmd.add("hls");
        cmd.add("-hls_time");
        cmd.add(String.valueOf(properties.getFfmpeg().getSegmentDuration()));
        cmd.add("-hls_list_size");
        cmd.add("0");
        cmd.add(m3u8Path.toString());

        try {
            runCommand(cmd);

            if (!Files.exists(m3u8Path)) {
                throw new BusinessException(ErrorCode.M3U8_GENERATION_FAILED,
                        "M3U8 file not generated: " + m3u8Path);
            }

            log.info("M3U8切片完成: {}", m3u8Path);
            return m3u8Path;
        } catch (BusinessException e) {
            if (e.getErrorCode() == ErrorCode.FFMPEG_COMMAND_FAILED ||
                    e.getErrorCode() == ErrorCode.FFMPEG_TIMEOUT) {
                throw new BusinessException(ErrorCode.SLICE_FAILED,
                        "Failed to slice video to M3U8: " + e.getMessage());
            }
            throw e;
        }
    }
}
