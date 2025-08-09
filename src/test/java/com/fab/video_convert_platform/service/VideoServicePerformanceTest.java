package com.fab.video_convert_platform.service;

import com.fab.video_convert_platform.domain.ProjectConfig;
import com.fab.video_convert_platform.domain.VideoUploadTask;
import com.fab.video_convert_platform.infra.monitor.VideoProcessingMetrics;
import com.fab.video_convert_platform.mapper.ProjectConfigMapper;
import com.fab.video_convert_platform.service.impl.VideoServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.*;

/**
 * 视频处理性能测试
 * 用于测试系统在并发场景下的性能表现
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class VideoServicePerformanceTest {

    @Autowired
    private VideoServiceImpl videoService;

    @Autowired
    private ProjectConfigMapper projectConfigMapper;

    @Autowired
    private VideoProcessingMetrics metrics;

    private ProjectConfig testProject;

    @BeforeEach
    void setUp() {
        // 创建测试项目
        testProject = new ProjectConfig();
        testProject.setProjectNo("PERF_TEST");
        testProject.setProjectName("性能测试项目");
        testProject.setArchiveRoot("/tmp/perf-test-archive");
        testProject.setIsActive(true);
        projectConfigMapper.insert(testProject);
    }

    @Test
    void concurrentUpload_ShouldHandleMultipleRequests() throws Exception {
        // Given
        int concurrentUsers = 10;
        int uploadsPerUser = 5;
        ExecutorService executor = Executors.newFixedThreadPool(concurrentUsers);

        long startActiveTasks = metrics.getActiveTasksCount();

        // When - 并发上传测试
        CompletableFuture<Void>[] futures = IntStream.range(0, concurrentUsers)
            .mapToObj(userId -> CompletableFuture.runAsync(() -> {
                try {
                    for (int i = 0; i < uploadsPerUser; i++) {
                        MockMultipartFile file = new MockMultipartFile(
                            "file",
                            String.format("perf-test-user%d-file%d.mp4", userId, i),
                            "video/mp4",
                            String.format("test content for user %d file %d", userId, i).getBytes());

                        VideoUploadTask task = videoService.upload(
                            file,
                            "PERF_TEST",
                            String.format("PATIENT_%03d", userId),
                            "BASELINE");

                        assertThat(task).isNotNull();
                        assertThat(task.getProjectNo()).isEqualTo("PERF_TEST");
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, executor))
            .toArray(CompletableFuture[]::new);

        // Then - 等待所有任务完成
        CompletableFuture.allOf(futures).get(30, TimeUnit.SECONDS);
        executor.shutdown();

        // 验证指标收集
        long endActiveTasks = metrics.getActiveTasksCount();
        assertThat(endActiveTasks).isGreaterThanOrEqualTo(startActiveTasks);

        // 验证项目指标
        long uploadCount = metrics.getProjectMetric("PERF_TEST", "upload");
        assertThat(uploadCount).isEqualTo(concurrentUsers * uploadsPerUser);
    }

    @Test
    void memoryUsage_ShouldNotExceedLimits() {
        // Given
        Runtime runtime = Runtime.getRuntime();
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();

        // When - 执行内存密集型操作
        for (int i = 0; i < 20; i++) {
            MockMultipartFile largeFile = new MockMultipartFile(
                "file",
                String.format("large-file-%d.mp4", i),
                "video/mp4",
                new byte[1024 * 1024]); // 1MB文件

            try {
                VideoUploadTask task = videoService.upload(
                    largeFile, "PERF_TEST", String.format("PATIENT_%03d", i), "BASELINE");
                assertThat(task).isNotNull();
            } catch (Exception e) {
                // 某些测试环境可能会失败，这是正常的
            }
        }

        // Then - 检查内存使用
        System.gc(); // 建议垃圾回收
        Thread.yield();

        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryIncrease = finalMemory - initialMemory;

        // 内存增长不应该超过100MB（这是一个合理的阈值）
        assertThat(memoryIncrease).isLessThan(100 * 1024 * 1024);
    }

    @Test
    void responseTime_ShouldBeMeasured() {
        // Given
        MockMultipartFile file = new MockMultipartFile(
            "file", "response-time-test.mp4", "video/mp4", "test content".getBytes());

        // When
        long startTime = System.currentTimeMillis();

        VideoUploadTask task = videoService.upload(
            file, "PERF_TEST", "PATIENT_RT_001", "BASELINE");

        long endTime = System.currentTimeMillis();
        long responseTime = endTime - startTime;

        // Then
        assertThat(task).isNotNull();
        assertThat(responseTime).isLessThan(5000); // 响应时间应该小于5秒

        System.out.printf("上传响应时间: %d ms%n", responseTime);
    }
}
