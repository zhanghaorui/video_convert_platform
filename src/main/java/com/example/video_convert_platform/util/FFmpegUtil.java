package com.example.video_convert_platform.util;

import com.example.video_convert_platform.common.BusinessException;
import com.example.video_convert_platform.common.ErrorCode;
import com.example.video_convert_platform.config.VideoProcessingProperties;
import com.example.video_convert_platform.domain.service.VideoProcessor;
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
 * 支持分阶段超时策略，根据视频时长动态计算超时时间。
 * 实现 VideoProcessor 接口，提供视频处理核心能力。
 */
@Slf4j
@Component
@SuppressWarnings("EI_EXPOSE_REP2") // 依赖注入场景，预期行为
public class FFmpegUtil implements VideoProcessor {

    @SuppressWarnings("EI_EXPOSE_REP2") // 依赖注入场景，预期行为
    private final VideoProcessingProperties properties;

    public FFmpegUtil(VideoProcessingProperties properties) {
        this.properties = properties;
    }

    /**
     * 获取视频时长（秒）
     * 使用 ffprobe 快速探测，失败时返回默认值
     */
    public double getVideoDurationSeconds(Path input) {
        if (input == null || !Files.exists(input)) {
            log.warn("视频文件不存在或路径为空，使用默认时长 300秒");
            return 300.0;
        }

        List<String> cmd = new ArrayList<>();
        cmd.add("ffprobe");
        cmd.add("-v");
        cmd.add("error");
        cmd.add("-show_entries");
        cmd.add("format=duration");
        cmd.add("-of");
        cmd.add("default=noprint_wrappers=1:nokey=1");
        cmd.add(input.toString());

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        try {
            Process process = pb.start();
            boolean finished = process.waitFor(5000, TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                log.warn("获取视频时长超时，使用默认值 300秒");
                return 300.0;
            }

            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line = br.readLine();
                if (line != null && !line.trim().isEmpty() && !"N/A".equalsIgnoreCase(line.trim())) {
                    double duration = Double.parseDouble(line.trim());
                    log.info("视频时长: {} 秒 ({})", duration, input);
                    return duration;
                }
            }
        } catch (IOException | InterruptedException | NumberFormatException e) {
            log.warn("获取视频时长失败: {}, 使用默认值 300秒", e.getMessage());
        }

        return 300.0;
    }

    /**
     * 计算转码操作动态超时时间
     * 公式: base_timeout + video_duration_minutes * transcode_timeout_per_minute
     */
    public long computeTranscodeTimeout(double videoDurationSeconds) {
        double minutes = videoDurationSeconds / 60.0;
        long baseTimeout = properties.getFfmpeg().getTimeout();
        long perMinute = properties.getFfmpeg().getTranscodeTimeoutPerMinute();
        long dynamicTimeout = baseTimeout + (long) (minutes * perMinute);
        // 设置上限：不超过 2 小时
        long maxTimeout = 7200000L;
        if (dynamicTimeout > maxTimeout) {
            dynamicTimeout = maxTimeout;
        }
        log.info("转码动态超时: {}ms (视频时长={}分钟)", dynamicTimeout, minutes);
        return dynamicTimeout;
    }

    /**
     * 计算切片操作动态超时时间
     * 公式: base_timeout + video_duration_minutes * slice_timeout_per_minute
     */
    public long computeSliceTimeout(double videoDurationSeconds) {
        double minutes = videoDurationSeconds / 60.0;
        long baseTimeout = properties.getFfmpeg().getTimeout();
        long perMinute = properties.getFfmpeg().getSliceTimeoutPerMinute();
        long dynamicTimeout = baseTimeout + (long) (minutes * perMinute);
        // 设置上限：不超过 1 小时
        long maxTimeout = 3600000L;
        if (dynamicTimeout > maxTimeout) {
            dynamicTimeout = maxTimeout;
        }
        log.info("切片动态超时: {}ms (视频时长={}分钟)", dynamicTimeout, minutes);
        return dynamicTimeout;
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
            // 验证操作使用固定超时
            runCommand(cmd, properties.getFfmpeg().getValidateTimeout());
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
     * 快速验证视频文件（仅检查前几秒）
     * 用于大文件的快速完整性检查，避免全文件扫描超时
     *
     * @param input 输入视频文件
     * @param durationSeconds 检查的时长（秒），默认建议10-30秒
     */
    public void validateQuick(Path input, int durationSeconds) throws IOException, InterruptedException {
        log.info("开始快速验证视频文件（前{}秒）: {}", durationSeconds, input);

        if (!Files.exists(input)) {
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND,
                    "Video file not found: " + input);
        }

        List<String> cmd = new ArrayList<>();
        cmd.add(properties.getFfmpeg().getExecutablePath());
        cmd.add("-v");
        cmd.add("error");
        cmd.add("-t");
        cmd.add(String.valueOf(durationSeconds));  // 只读取前N秒
        cmd.add("-i");
        cmd.add(input.toString());
        cmd.add("-f");
        cmd.add("null");
        cmd.add("-");

        try {
            // 快速验证使用固定超时
            runCommand(cmd, properties.getFfmpeg().getValidateQuickTimeout());
            log.info("视频文件快速验证通过: {}", input);
        } catch (BusinessException e) {
            if (e.getErrorCode() == ErrorCode.FFMPEG_COMMAND_FAILED) {
                throw new BusinessException(ErrorCode.VIDEO_CORRUPTED,
                        "Video file is corrupted or unreadable: " + input);
            }
            throw e;
        }
    }

    /**
     * Convert AVI video to MP4 using H.264/AAC codecs.
     * 对于高码率视频（如DV格式 >20Mbps），先降低码率再转换，大幅提升处理速度
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

        // 检测原始码率
        Integer originBitrateKbps = null;
        try {
            originBitrateKbps = getVideoBitrateKbps(input);
            if (originBitrateKbps != null && originBitrateKbps <= 0) {
                originBitrateKbps = null;
            }
        } catch (Exception e) {
            log.warn("获取原始码率失败，使用默认策略: {}", e.getMessage());
        }

        // 高码率阈值：20 Mbps (20000 kbps) - DV格式通常在48Mbps左右
        final int HIGH_BITRATE_THRESHOLD = 20000;

        Path actualInput = input;
        Path tempPreprocessed = null;

        // 如果码率超过阈值，先进行降码率预处理
        if (originBitrateKbps != null && originBitrateKbps > HIGH_BITRATE_THRESHOLD) {
            log.warn("检测到高码率视频: {}kbps (>{}kbps)，先进行降码率预处理以加快转换速度",
                     originBitrateKbps, HIGH_BITRATE_THRESHOLD);

            // 创建临时预处理文件
            tempPreprocessed = input.getParent().resolve("temp_preprocessed_" + System.currentTimeMillis() + ".mp4");

            try {
                // 使用快速预设降低码率到合理范围 (12 Mbps)
                preprocessHighBitrateVideo(input, tempPreprocessed, 12000);
                actualInput = tempPreprocessed;
                log.info("高码率视频预处理完成，使用预处理文件继续转换");
            } catch (Exception e) {
                log.error("预处理失败，回退使用原始文件: {}", e.getMessage());
                // 清理临时文件
                if (tempPreprocessed != null && Files.exists(tempPreprocessed)) {
                    Files.deleteIfExists(tempPreprocessed);
                }
                actualInput = input;
            }
        }

        try {
            // 确定最终目标码率
            int fallbackBitrateKbps = 8000;
            int targetBitrateKbps = (originBitrateKbps != null && originBitrateKbps <= HIGH_BITRATE_THRESHOLD)
                                    ? originBitrateKbps : fallbackBitrateKbps;

            // 限制上限为 15 Mbps（已经通过预处理降低了高码率）
            if (targetBitrateKbps > 15000) {
                log.info("目标码率{}kbps超出上限，截断为15Mbps", targetBitrateKbps);
                targetBitrateKbps = 15000;
            }

            log.info("AVI转MP4采用目标码率: {} kbps (原始={} kbps)", targetBitrateKbps, originBitrateKbps);

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
            cmd.add(actualInput.toString());
            cmd.add("-c:v");
            if (properties.getFfmpeg().isUseVideoToolbox()) {
                cmd.add("h264_videotoolbox");
                cmd.add("-b:v");
                cmd.add(targetBitrateKbps + "k");
                cmd.add("-maxrate");
                cmd.add(targetBitrateKbps + "k");
                cmd.add("-bufsize");
                cmd.add((targetBitrateKbps * 2) + "k");
            } else {
                cmd.add("libx264");
                cmd.add("-preset");
                cmd.add("medium");
                cmd.add("-profile:v");
                cmd.add("high");
                cmd.add("-level");
                cmd.add("4.1");
                cmd.add("-pix_fmt");
                cmd.add("yuv420p");
                cmd.add("-b:v");
                cmd.add(targetBitrateKbps + "k");
                cmd.add("-maxrate");
                cmd.add(targetBitrateKbps + "k");
                cmd.add("-bufsize");
                cmd.add((targetBitrateKbps * 2) + "k");
            }
            cmd.add("-c:a");
            cmd.add("aac");
            cmd.add("-threads");
            cmd.add(String.valueOf(properties.getFfmpeg().getThreads()));
            cmd.add("-movflags");
            cmd.add("+faststart");
            cmd.add(output.toString());

            // AVI转MP4使用动态超时（根据视频时长计算）
            double videoDuration = getVideoDurationSeconds(actualInput);
            long dynamicTimeout = computeTranscodeTimeout(videoDuration);
            runCommand(cmd, dynamicTimeout);
            log.info("AVI转MP4完成: {} (最终码率={}kbps)", output, targetBitrateKbps);

        } finally {
            // 清理临时预处理文件
            if (tempPreprocessed != null && Files.exists(tempPreprocessed)) {
                try {
                    Files.deleteIfExists(tempPreprocessed);
                    log.info("已清理临时预处理文件: {}", tempPreprocessed);
                } catch (IOException e) {
                    log.warn("清理临时文件失败: {}", e.getMessage());
                }
            }
        }
    }

    /**
     * 对高码率视频进行快速降码率预处理
     * 使用 ultrafast 预设和适中的码率，快速完成第一遍转码
     */
    private void preprocessHighBitrateVideo(Path input, Path output, int targetBitrateKbps)
            throws IOException, InterruptedException {
        log.info("开始预处理高码率视频: {} -> {} (目标码率={}kbps)", input, output, targetBitrateKbps);

        List<String> cmd = new ArrayList<>();
        cmd.add(properties.getFfmpeg().getExecutablePath());
        cmd.add("-i");
        cmd.add(input.toString());
        cmd.add("-c:v");
        cmd.add("libx264");
        cmd.add("-preset");
        cmd.add("ultrafast");
        // 使用2-pass式的严格码率控制
        cmd.add("-b:v");
        cmd.add(targetBitrateKbps + "k");
        cmd.add("-maxrate");
        cmd.add(targetBitrateKbps + "k");
        cmd.add("-bufsize");
        cmd.add((targetBitrateKbps / 2) + "k");  // 减小buffer，更严格控制
        cmd.add("-x264-params");
        cmd.add("nal-hrd=cbr");  // 强制恒定码率
        cmd.add("-c:a");
        cmd.add("copy");
        cmd.add("-threads");
        cmd.add(String.valueOf(properties.getFfmpeg().getThreads()));
        cmd.add("-y");
        cmd.add(output.toString());

        // 预处理使用动态超时
        double videoDuration = getVideoDurationSeconds(input);
        long dynamicTimeout = computeTranscodeTimeout(videoDuration);
        runCommand(cmd, dynamicTimeout);
        log.info("高码率视频预处理完成: {}", output);
    }

    /**
     * Probe video resolution using ffprobe with timeout.
     */
    public int[] getResolution(Path input) throws IOException, InterruptedException {
        VideoStreamInfo info = probeVideoStreamInfo(input);
        return info.getDisplayResolution();
    }

    /**
     * Probe video stream geometry and rotation metadata using ffprobe with timeout.
     */
    public VideoStreamInfo probeVideoStreamInfo(Path input) throws IOException, InterruptedException {
        log.info("获取视频流信息: {}", input);

        List<String> cmd = new ArrayList<>();
        cmd.add("ffprobe");
        cmd.add("-v");
        cmd.add("error");
        cmd.add("-select_streams");
        cmd.add("v:0");
        cmd.add("-show_entries");
        cmd.add("stream=width,height:stream_tags=rotate:stream_side_data=rotation");
        cmd.add("-of");
        cmd.add("default=noprint_wrappers=1");
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

        VideoStreamInfo info = parseVideoStreamInfo(result);
        log.info("视频流信息: coded={}x{}, display={}x{}, rotation={}度",
                info.getCodedWidth(), info.getCodedHeight(),
                info.getDisplayWidth(), info.getDisplayHeight(),
                info.getRotationDegrees());
        return info;
    }

    /**
     * Transcode input video into mp4 with specified scale.
     * Note: For hardware encoder (h264_videotoolbox), a target bitrate will be applied.
     */
    public void transcode(Path input, Path output, int width, int height)
            throws IOException, InterruptedException {
        transcode(input, output, width, height, null);
    }

    /**
     * Transcode input video with specified scale and optional target bitrate (kbps).
     * When using VideoToolbox, the provided targetBitrateKbps (if not null) will be used as "-b:v".
     * For libx264 (software), CRF mode is used and targetBitrateKbps is ignored.
     */
    public void transcode(Path input, Path output, int width, int height, Integer targetBitrateKbps)
            throws IOException, InterruptedException {
        // 输入参数验证
        if (input == null) {
            throw new IllegalArgumentException("输入文件路径不能为空");
        }
        if (output == null) {
            throw new IllegalArgumentException("输出文件路径不能为空");
        }
        
        log.info("开始转码视频: {} -> {} ({}x{}), targetBitrateKbps={} (仅硬编或软编显式码率)", input, output, width, height, targetBitrateKbps);

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
        cmd.add(buildScaleFilter(width, height));
        cmd.add("-c:v");
        if (properties.getFfmpeg().isUseVideoToolbox()) {
            cmd.add("h264_videotoolbox");
            int kbps = (targetBitrateKbps != null && targetBitrateKbps > 0) ? targetBitrateKbps : 4000;
            cmd.add("-b:v");
            cmd.add(kbps + "k");
        } else {
            cmd.add("libx264");
            cmd.add("-preset");
            cmd.add("medium");
            cmd.add("-profile:v");
            cmd.add("high");
            cmd.add("-level");
            cmd.add("4.1");
            cmd.add("-pix_fmt");
            cmd.add("yuv420p");
            if (targetBitrateKbps != null && targetBitrateKbps > 0) {
                int kbps = targetBitrateKbps;
                cmd.add("-b:v");
                cmd.add(kbps + "k");
                cmd.add("-maxrate");
                cmd.add(kbps + "k");
                cmd.add("-bufsize");
                cmd.add((kbps * 2) + "k");
            } else {
                cmd.add("-crf");
                cmd.add("23");
            }
        }
        cmd.add("-threads");
        cmd.add(String.valueOf(properties.getFfmpeg().getThreads()));
        cmd.add("-metadata:s:v:0");
        cmd.add("rotate=0");
        cmd.add(output.toString());

        try {
            // 转码操作使用动态超时（根据视频时长计算）
            double videoDuration = getVideoDurationSeconds(input);
            long dynamicTimeout = computeTranscodeTimeout(videoDuration);
            runCommand(cmd, dynamicTimeout);
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
        long startTime = System.currentTimeMillis();
        log.info("开始切片生成M3U8: {} -> {}", input, outputDir);

        Files.createDirectories(outputDir);
        Path m3u8Path = outputDir.resolve("index.m3u8");

        List<String> cmd = new ArrayList<>();
        cmd.add(properties.getFfmpeg().getExecutablePath());
        cmd.add("-i");
        cmd.add(input.toString());
        cmd.add("-c:v");
        cmd.add("copy");
        cmd.add("-an");  // 去掉音轨
        cmd.add("-f");
        cmd.add("hls");
        cmd.add("-hls_time");
        cmd.add(String.valueOf(properties.getFfmpeg().getSegmentDuration()));
        cmd.add("-hls_list_size");
        cmd.add("0");
        cmd.add(m3u8Path.toString());

        try {
            // 切片操作使用动态超时（根据视频时长计算）
            double videoDuration = getVideoDurationSeconds(input);
            long dynamicTimeout = computeSliceTimeout(videoDuration);
            runCommand(cmd, dynamicTimeout);

            if (!Files.exists(m3u8Path)) {
                throw new BusinessException(ErrorCode.M3U8_GENERATION_FAILED,
                        "M3U8 file not generated: " + m3u8Path);
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("M3U8切片完成: {}, 耗时: {}ms ({}秒)", m3u8Path, duration, duration / 1000.0);
            return m3u8Path;
        } catch (BusinessException e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("M3U8切片失败, 耗时: {}ms ({}秒)", duration, duration / 1000.0);
            if (e.getErrorCode() == ErrorCode.FFMPEG_COMMAND_FAILED ||
                    e.getErrorCode() == ErrorCode.FFMPEG_TIMEOUT) {
                throw new BusinessException(ErrorCode.SLICE_FAILED,
                        "Failed to slice video to M3U8: " + e.getMessage());
            }
            throw e;
        }
    }

    /**
     * 获取视频流原始码率（kbps）。
     * 优先取视频流 bit_rate；若不可用，再尝试取容器格式 bit_rate。
     */
    public Integer getVideoBitrateKbps(Path input) throws IOException, InterruptedException {
        if (input == null) {
            return null;
        }
        // 先取视频流码率
        Integer streamKbps = probeBitrate(input, true);
        if (streamKbps != null && streamKbps > 0) {
            return streamKbps;
        }
        // 退化到容器总码率
        return probeBitrate(input, false);
    }

    private Integer probeBitrate(Path input, boolean stream) throws IOException, InterruptedException {
        List<String> cmd = new ArrayList<>();
        cmd.add("ffprobe");
        cmd.add("-v");
        cmd.add("error");
        if (stream) {
            cmd.add("-select_streams");
            cmd.add("v:0");
            cmd.add("-show_entries");
            cmd.add("stream=bit_rate");
        } else {
            cmd.add("-show_entries");
            cmd.add("format=bit_rate");
        }
        cmd.add("-of");
        cmd.add("default=noprint_wrappers=1:nokey=1");
        cmd.add(input.toString());

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        boolean finished = process.waitFor(8000, java.util.concurrent.TimeUnit.MILLISECONDS);
        if (!finished) {
            process.destroyForcibly();
            return null;
        }
        try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {
            String line = br.readLine();
            if (line == null) {
                return null;
            }
            line = line.trim();
            if (line.isEmpty() || line.equalsIgnoreCase("N/A")) {
                return null;
            }
            try {
                long bps = Long.parseLong(line);
                if (bps <= 0) {
                    return null;
                }
                int kbps = (int) Math.round(bps / 1000.0);
                log.info("探测到{}码率: {} kbps ({} bps)", stream ? "视频流" : "容器", kbps, bps);
                return kbps;
            } catch (NumberFormatException e) {
                log.warn("解析码率失败: {}", line);
                return null;
            }
        }
    }

    static VideoStreamInfo parseVideoStreamInfo(String result) {
        if (result == null || result.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.VIDEO_RESOLUTION_ERROR,
                    "Unexpected ffprobe output format: '" + result + "'");
        }

        Integer width = null;
        Integer height = null;
        int rotation = 0;
        String[] lines = result.split("\\r?\\n");
        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("[") || !line.contains("=")) {
                continue;
            }

            int separator = line.indexOf('=');
            String key = line.substring(0, separator).trim();
            String value = line.substring(separator + 1).trim();
            if ("width".equals(key) && width == null) {
                width = parsePositiveInteger(value, "width", result);
            } else if ("height".equals(key) && height == null) {
                height = parsePositiveInteger(value, "height", result);
            } else if ("rotation".equals(key) || "rotate".equals(key) || key.endsWith(":rotate")) {
                rotation = normalizeRotationDegrees(value);
            }
        }

        if (width == null || height == null) {
            throw new BusinessException(ErrorCode.VIDEO_RESOLUTION_ERROR,
                    "Unexpected ffprobe output format: '" + result + "'");
        }

        return new VideoStreamInfo(width, height, rotation);
    }

    public static int[] orientTargetDimensions(int width, int height, int displayWidth, int displayHeight) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("目标宽高必须大于0");
        }
        if (displayWidth > 0 && displayHeight > 0 && displayWidth < displayHeight && width > height) {
            return new int[]{height, width};
        }
        return new int[]{width, height};
    }

    public static int[] computeBoundedDisplayDimensions(int displayWidth, int displayHeight,
                                                        int landscapeMaxWidth, int landscapeMaxHeight) {
        if (displayWidth <= 0 || displayHeight <= 0 ||
                landscapeMaxWidth <= 0 || landscapeMaxHeight <= 0) {
            throw new IllegalArgumentException("宽高必须大于0");
        }

        int maxWidth = landscapeMaxWidth;
        int maxHeight = landscapeMaxHeight;
        if (displayWidth < displayHeight) {
            maxWidth = landscapeMaxHeight;
            maxHeight = landscapeMaxWidth;
        }

        if (displayWidth <= maxWidth && displayHeight <= maxHeight) {
            return new int[]{displayWidth, displayHeight};
        }

        double scale = Math.min(maxWidth * 1.0 / displayWidth, maxHeight * 1.0 / displayHeight);
        int targetWidth = toEvenDimension((int) Math.round(displayWidth * scale));
        int targetHeight = toEvenDimension((int) Math.round(displayHeight * scale));
        return new int[]{targetWidth, targetHeight};
    }

    static String buildScaleFilter(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("目标宽高必须大于0");
        }
        return "scale=" + width + ":" + height + ",setsar=1";
    }

    private static int parsePositiveInteger(String value, String field, String fullOutput) {
        try {
            int parsed = Integer.parseInt(value);
            if (parsed <= 0) {
                throw new NumberFormatException(field + " must be positive");
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.VIDEO_RESOLUTION_ERROR,
                    "Malformed ffprobe output: non-numeric " + field + " '" + fullOutput
                            + "'. Error: " + e.getMessage());
        }
    }

    private static int normalizeRotationDegrees(String value) {
        try {
            int rounded = (int) Math.round(Double.parseDouble(value));
            return ((rounded % 360) + 360) % 360;
        } catch (NumberFormatException e) {
            log.warn("解析视频旋转角度失败: {}", value);
            return 0;
        }
    }

    private static int toEvenDimension(int dimension) {
        int adjusted = Math.max(2, dimension);
        return adjusted % 2 == 0 ? adjusted : adjusted - 1;
    }

    public static final class VideoStreamInfo {
        private final int codedWidth;
        private final int codedHeight;
        private final int rotationDegrees;

        public VideoStreamInfo(int codedWidth, int codedHeight, int rotationDegrees) {
            if (codedWidth <= 0 || codedHeight <= 0) {
                throw new IllegalArgumentException("视频宽高必须大于0");
            }
            this.codedWidth = codedWidth;
            this.codedHeight = codedHeight;
            this.rotationDegrees = ((rotationDegrees % 360) + 360) % 360;
        }

        public int getCodedWidth() {
            return codedWidth;
        }

        public int getCodedHeight() {
            return codedHeight;
        }

        public int getRotationDegrees() {
            return rotationDegrees;
        }

        public boolean hasRotationMetadata() {
            return rotationDegrees != 0;
        }

        public int getDisplayWidth() {
            return isQuarterTurn() ? codedHeight : codedWidth;
        }

        public int getDisplayHeight() {
            return isQuarterTurn() ? codedWidth : codedHeight;
        }

        public int[] getDisplayResolution() {
            return new int[]{getDisplayWidth(), getDisplayHeight()};
        }

        private boolean isQuarterTurn() {
            return rotationDegrees == 90 || rotationDegrees == 270;
        }
    }
}
