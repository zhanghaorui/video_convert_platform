package com.example.video_convert_platform.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 视频处理相关配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "video.processing")
public class VideoProcessingProperties {

    /**
     * 临时文件目录
     */
    private String tempDir = "/tmp/video-processing";

    /**
     * 分片大小
     */
    private String chunkSize = "10MB";

    /**
     * 最大分片数
     */
    private Integer maxChunks = 1000;

    /**
     * FFmpeg相关配置
     */
    private Ffmpeg ffmpeg = new Ffmpeg();

    // 防御性 getter/setter 方法
    public Ffmpeg getFfmpeg() {
        return ffmpeg != null ? new Ffmpeg(ffmpeg) : null;
    }

    public void setFfmpeg(Ffmpeg ffmpeg) {
        this.ffmpeg = ffmpeg != null ? new Ffmpeg(ffmpeg) : null;
    }

    @Data
    public static class Ffmpeg {
        /**
         * FFmpeg可执行文件路径
         */
        private String executablePath = "ffmpeg";

        /**
         * 基础超时时间(毫秒) - 作为最小超时时间
         */
        private Long timeout = 300000L;

        /**
         * 转码操作每分钟视频额外需要的时间(毫秒)
         * 转码最耗时，建议每分钟视频预留1分钟处理时间
         */
        private Long transcodeTimeoutPerMinute = 60000L;

        /**
         * 切片操作每分钟视频额外需要的时间(毫秒)
         * 切片使用 -c:v copy 不重新编码，相对较快
         */
        private Long sliceTimeoutPerMinute = 10000L;

        /**
         * 验证操作超时时间(毫秒) - 固定值
         */
        private Long validateTimeout = 60000L;

        /**
         * 快速验证超时时间(毫秒) - 固定值
         */
        private Long validateQuickTimeout = 60000L;

        /**
         * 并发线程数
         */
        private Integer threads = 2;

        /**
         * HLS切片时长(秒)
         */
        private Integer segmentDuration = 10;

        /**
         * 是否启用VideoToolbox硬件加速
         */
        private boolean useVideoToolbox = false;

        /**
         * 是否去除音轨（默认保留音轨）
         */
        private boolean removeAudio = false;

        /**
         * 质量配置
         */
        private Quality quality = new Quality();

        // 拷贝构造器
        public Ffmpeg() {}

        public Ffmpeg(Ffmpeg other) {
            if (other != null) {
                this.executablePath = other.executablePath;
                this.timeout = other.timeout;
                this.transcodeTimeoutPerMinute = other.transcodeTimeoutPerMinute;
                this.sliceTimeoutPerMinute = other.sliceTimeoutPerMinute;
                this.validateTimeout = other.validateTimeout;
                this.validateQuickTimeout = other.validateQuickTimeout;
                this.threads = other.threads;
                this.segmentDuration = other.segmentDuration;
                this.useVideoToolbox = other.useVideoToolbox;
                this.removeAudio = other.removeAudio;
                this.quality = other.quality != null ? new Quality(other.quality) : null;
            }
        }

        // 防御性 getter/setter 方法
        public Quality getQuality() {
            return quality != null ? new Quality(quality) : null;
        }

        public void setQuality(Quality quality) {
            this.quality = quality != null ? new Quality(quality) : null;
        }

        @Data
        public static class Quality {
            /**
             * 低画质
             */
            private String low = "480p";

            /**
             * 标准画质
             */
            private String standard = "720p";

            // 拷贝构造器
            public Quality() {}

            public Quality(Quality other) {
                if (other != null) {
                    this.low = other.low;
                    this.standard = other.standard;
                }
            }
        }
    }
}
