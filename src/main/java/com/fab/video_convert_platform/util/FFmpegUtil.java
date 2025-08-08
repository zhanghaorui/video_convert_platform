package com.fab.video_convert_platform.util;

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

import lombok.extern.slf4j.Slf4j;

/**
 * Helper utilities for invoking FFmpeg command line.
 */
@Slf4j
public class FFmpegUtil {

    private FFmpegUtil() {
    }

    /**
     * Execute given command and ensure it exits with 0.
     */
    public static void runCommand(List<String> command) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.info(line);
            }
        }
        int code = p.waitFor();
        if (code != 0) {
            throw new IOException("ffmpeg command failed with code " + code);
        }
    }

    /**
     * Validate video file integrity by invoking ffmpeg with error reporting.
     */
    public static void validate(Path input) throws IOException, InterruptedException {
        List<String> cmd = new ArrayList<>();
        cmd.add("ffmpeg");
        cmd.add("-v");
        cmd.add("error");
        cmd.add("-i");
        cmd.add(input.toString());
        cmd.add("-f");
        cmd.add("null");
        cmd.add("-");
        runCommand(cmd);
    }

    /**
     * Convert AVI video to MP4 using H.264/AAC codecs.
     */
    public static void aviToMp4(Path input, Path output) throws IOException, InterruptedException {
        Files.createDirectories(output.getParent());
        List<String> cmd = new ArrayList<>();
        cmd.add("ffmpeg");
        cmd.add("-i");
        cmd.add(input.toString());
        cmd.add("-c:v");
        cmd.add("libx264");
        cmd.add("-c:a");
        cmd.add("aac");
        cmd.add(output.toString());
        runCommand(cmd);
    }

    /**
     * Probe video resolution using ffprobe.
     */
    public static int[] getResolution(Path input) throws IOException, InterruptedException {
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
        Process p = pb.start();
        String result;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             InputStream in = p.getInputStream()) {
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) != -1) {
                baos.write(buf, 0, len);
            }
            result = baos.toString(StandardCharsets.UTF_8.name()).trim();
        }
        int code = p.waitFor();
        if (code != 0) {
            throw new IOException("ffprobe command failed with code " + code);
        }
        String[] parts = result.split("x");
        if (parts.length != 2) {
            throw new IOException("Unexpected ffprobe output format: '" + result + "'");
        }
        int w, h;
        try {
            w = Integer.parseInt(parts[0].trim());
            h = Integer.parseInt(parts[1].trim());
        } catch (NumberFormatException e) {
            throw new IOException("Malformed ffprobe output: non-numeric resolution '" + result + "'", e);
        }
        return new int[]{w, h};
    }

    /**
     * Transcode input video into mp4 with specified scale.
     */
    public static void transcode(Path input, Path output, int width, int height)
            throws IOException, InterruptedException {
        Files.createDirectories(output.getParent());
        List<String> cmd = new ArrayList<>();
        cmd.add("ffmpeg");
        cmd.add("-i");
        cmd.add(input.toString());
        cmd.add("-vf");
        cmd.add("scale=" + width + ":" + height);
        cmd.add("-c:v");
        cmd.add("libx264");
        cmd.add("-preset");
        cmd.add("medium");
        cmd.add("-crf");
        cmd.add("23");
        cmd.add(output.toString());
        runCommand(cmd);
    }

    /**
     * Slice mp4 file into HLS m3u8 under target directory.
     */
    public static Path sliceToM3u8(Path input, Path outputDir)
            throws IOException, InterruptedException {
        Files.createDirectories(outputDir);
        Path m3u8 = outputDir.resolve(com.fab.video_convert_platform.common.VideoConstants.M3U8_NAME);
        List<String> cmd = new ArrayList<>();
        cmd.add("ffmpeg");
        cmd.add("-i");
        cmd.add(input.toString());
        cmd.add("-c:v");
        cmd.add("copy");
        cmd.add("-an");
        cmd.add("-f");
        cmd.add("hls");
        cmd.add("-hls_time");
        cmd.add("10");
        cmd.add("-hls_list_size");
        cmd.add("0");
        cmd.add(m3u8.toString());
        runCommand(cmd);
        return m3u8;
    }
}
