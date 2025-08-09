package com.fab.video_convert_platform.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 测试环境专用配置
 */
@TestConfiguration
@Profile("test")
public class TestConfig {

    /**
     * 测试环境的FFmpeg工具类模拟
     */
    @Bean
    @Primary
    public com.fab.video_convert_platform.util.FFmpegUtil mockFFmpegUtil(
            VideoProcessingProperties properties) {
        return new com.fab.video_convert_platform.util.FFmpegUtil(properties) {

            private final AtomicLong counter = new AtomicLong(0);

            @Override
            public void validate(Path input) throws IOException, InterruptedException {
                // 模拟验证，快速返回
                if (!Files.exists(input)) {
                    throw new IOException("File not found: " + input);
                }
                Thread.sleep(10); // 模拟处理时间
            }

            @Override
            public void aviToMp4(Path input, Path output) throws IOException, InterruptedException {
                // 模拟转换，创建输出文件
                Files.createDirectories(output.getParent());
                Files.copy(input, output);
                Thread.sleep(50); // 模拟处理时间
            }

            @Override
            public int[] getResolution(Path input) throws IOException, InterruptedException {
                // 返回固定分辨率用于测试
                Thread.sleep(10);
                return new int[]{1920, 1080};
            }

            @Override
            public void transcode(Path input, Path output, int width, int height)
                    throws IOException, InterruptedException {
                // 模拟转码
                Files.createDirectories(output.getParent());
                Files.copy(input, output);
                Thread.sleep(100); // 模拟处理时间
            }

            @Override
            public Path sliceToM3u8(Path input, Path outputDir) throws IOException, InterruptedException {
                // 模拟切片，创建m3u8文件
                Files.createDirectories(outputDir);
                Path m3u8Path = outputDir.resolve("index.m3u8");

                String m3u8Content = String.format(
                    "#EXTM3U\n#EXT-X-VERSION:3\n#EXT-X-TARGETDURATION:10\n" +
                    "#EXTINF:10.0,\nsegment%d_0.ts\n#EXTINF:10.0,\nsegment%d_1.ts\n#EXT-X-ENDLIST\n",
                    counter.incrementAndGet(), counter.get());

                Files.write(m3u8Path, m3u8Content.getBytes());
                Thread.sleep(200); // 模拟处理时间

                return m3u8Path;
            }
        };
    }
}
