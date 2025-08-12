package com.fab.video_convert_platform.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 性能监控配置
 * 
 * @author zhanghaorui
 */
@Configuration
public class MetricsConfig {

    @Bean
    public Timer videoProcessingTimer(MeterRegistry meterRegistry) {
        return Timer.builder("video.processing.duration")
                .description("视频处理耗时")
                .register(meterRegistry);
    }

    @Bean
    public Timer fileUploadTimer(MeterRegistry meterRegistry) {
        return Timer.builder("file.upload.duration")
                .description("文件上传耗时")
                .register(meterRegistry);
    }

    @Bean
    public Timer databaseQueryTimer(MeterRegistry meterRegistry) {
        return Timer.builder("database.query.duration")
                .description("数据库查询耗时")
                .register(meterRegistry);
    }
}
