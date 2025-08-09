package com.fab.video_convert_platform.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
/**
 * 业务相关配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "business")
public class BusinessProperties {

    /**
     * 回调相关配置
     */
    private Callback callback = new Callback();

    /**
     * 归档相关配置
     */
    private Archive archive = new Archive();

    @Data
    public static class Callback {
        /**
         * 重试次数
         */
        private Integer retryTimes = 3;

        /**
         * 超时时间(毫秒)
         */
        private Long timeout = 30000L;
    }

    @Data
    public static class Archive {
        /**
         * 是否启用MD5校验
         */
        private Boolean enableMd5Check = true;

        /**
         * 是否自动清理临时文件
         */
        private Boolean cleanupTempFiles = true;

        /**
         * 批处理大小
         */
        private Integer batchSize = 50;

        /**
         * 并发处理线程数
         */
        private Integer parallelWorkers = 4;
    }
}

