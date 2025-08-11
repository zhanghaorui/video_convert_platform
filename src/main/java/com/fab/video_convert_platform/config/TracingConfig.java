package com.fab.video_convert_platform.config;

import brave.sampler.Sampler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for distributed tracing sampler.
 */
@Configuration
public class TracingConfig {

    @Bean
    public Sampler defaultSampler(@Value("${spring.sleuth.sampler.probability:0.1}") float probability) {
        return Sampler.create(probability);
    }
}
