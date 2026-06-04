package com.example.video_convert_platform.maintenance;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 清理任务相关配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "maintenance.cleanup")
public class CleanupProperties {
    /** 是否启用清理 */
    private boolean enabled = true;
    /** Cron 表达式（若通过 @Scheduled 占位符引用） */
    private String cron = "0 0/30 * * * ?";
    /** 孤立分片目录 TTL（分钟） */
    private long orphanChunkTtlMinutes = 60;
    /** 孤立原始文件 TTL（小时） */
    private long orphanOriginalTtlHours = 24;
    /** Dry-run 模式：只记录日志，不实际删除 */
    private boolean dryRun = true;
}

