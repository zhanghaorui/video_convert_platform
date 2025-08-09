package com.fab.video_convert_platform.config;

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

    @Data
    public static class Ffmpeg {
        /**
         * FFmpeg可执行文件路径
         */
        private String executablePath = "ffmpeg";

        /**
         * 处理超时时间(毫秒)
         */
        private Long timeout = 300000L;

        /**
         * 并发线程数
         */
        private Integer threads = 2;

        /**
         * HLS切片时长(秒)
         */
        private Integer segmentDuration = 10;

        /**
         * 质量配置
         */
        private Quality quality = new Quality();

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
        }
    }
}
