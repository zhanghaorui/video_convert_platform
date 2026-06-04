package com.example.video_convert_platform.config;

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

    // 防御性 getter/setter 方法
    public Callback getCallback() {
        return callback != null ? new Callback(callback) : null;
    }

    public void setCallback(Callback callback) {
        this.callback = callback != null ? new Callback(callback) : null;
    }

    public Archive getArchive() {
        return archive != null ? new Archive(archive) : null;
    }

    public void setArchive(Archive archive) {
        this.archive = archive != null ? new Archive(archive) : null;
    }

    @Data
    public static class Callback {
        /**
         * Whether optional outbound webhook notification is enabled.
         */
        private Boolean enabled = false;

        /**
         * 重试次数
         */
        private Integer retryTimes = 3;

        /**
         * 超时时间(毫秒)
         */
        private Long timeout = 30000L;

        // 拷贝构造器
        public Callback() {}

        public Callback(Callback other) {
            if (other != null) {
                this.enabled = other.enabled;
                this.retryTimes = other.retryTimes;
                this.timeout = other.timeout;
            }
        }
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

        // 拷贝构造器
        public Archive() {}

        public Archive(Archive other) {
            if (other != null) {
                this.enableMd5Check = other.enableMd5Check;
                this.cleanupTempFiles = other.cleanupTempFiles;
                this.batchSize = other.batchSize;
                this.parallelWorkers = other.parallelWorkers;
            }
        }
    }
}
