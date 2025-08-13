package com.fab.video_convert_platform.interfaces.rest;

import com.fab.video_convert_platform.common.ApiResponse;
import com.fab.video_convert_platform.infra.monitor.VideoProcessingMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 监控数据API控制器
 * 提供业务监控指标查询接口
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/monitor")
@RequiredArgsConstructor
public class MonitorController {

    private final VideoProcessingMetrics metrics;

    /**
     * 获取系统概览指标
     */
    @GetMapping("/overview")
    public ApiResponse<Map<String, Object>> getSystemOverview() {
        Map<String, Object> overview = new HashMap<>();

        overview.put("activeTasks", metrics.getActiveTasksCount());
        overview.put("systemStatus", metrics.getActiveTasksCount() > 50 ? "HIGH_LOAD" : "NORMAL");
        overview.put("timestamp", System.currentTimeMillis());

        return ApiResponse.success(overview);
    }

    /**
     * 获取项目维度的处理统计
     */
    @GetMapping("/project-stats")
    public ApiResponse<Map<String, Object>> getProjectStats(
            @RequestParam(required = false) String projectNo) {

        Map<String, Object> stats = new HashMap<>();

        if (projectNo != null) {
            // 获取特定项目的统计
            stats.put("projectNo", projectNo);
            stats.put("uploadCount", metrics.getProjectMetric(projectNo, "upload"));
            stats.put("chunkCount", metrics.getProjectMetric(projectNo, "chunk"));
            stats.put("successCount", metrics.getProjectMetric(projectNo, "success"));
            stats.put("failureCount", metrics.getProjectMetric(projectNo, "failure"));
        } else {
            // 返回系统级别统计
            stats.put("totalActiveTasks", metrics.getActiveTasksCount());
            stats.put("systemLoad", metrics.getActiveTasksCount() > 20 ? "HIGH" : "NORMAL");
        }

        return ApiResponse.success(stats);
    }

    /**
     * 获取性能指标
     */
    @GetMapping("/performance")
    public ApiResponse<Map<String, Object>> getPerformanceMetrics() {
        Map<String, Object> performance = new HashMap<>();

        // 基本性能指标
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;

        Map<String, String> memoryInfo = new HashMap<>();
        memoryInfo.put("total", totalMemory / 1024 / 1024 + "MB");
        memoryInfo.put("used", usedMemory / 1024 / 1024 + "MB");
        memoryInfo.put("free", freeMemory / 1024 / 1024 + "MB");
        memoryInfo.put("usagePercent", String.format("%.2f%%", (double) usedMemory / totalMemory * 100));

        performance.put("memory", memoryInfo);
        performance.put("activeTasks", metrics.getActiveTasksCount());
        performance.put("availableProcessors", Runtime.getRuntime().availableProcessors());

        return ApiResponse.success(performance);
    }
}
