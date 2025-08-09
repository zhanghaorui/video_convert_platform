package com.fab.video_convert_platform.config;

import com.fab.video_convert_platform.infra.SliceTask;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for local slice task queue and executor.
 */
@Configuration
public class SliceExecutorConfig {

    @Bean
    public BlockingQueue<SliceTask> localSliceQueue() {
        return new ArrayBlockingQueue<>(200);
    }

    @Bean
    public ThreadPoolExecutor sliceExecutor() {
        return new ThreadPoolExecutor(
            6, 6,
            0L, TimeUnit.MILLISECONDS,
            new SynchronousQueue<>(),
            new ThreadPoolExecutor.CallerRunsPolicy());
    }
}
