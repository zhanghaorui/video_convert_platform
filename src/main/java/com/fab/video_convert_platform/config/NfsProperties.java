package com.fab.video_convert_platform.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * NFS存储相关配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "nfs")
public class NfsProperties {

    /**
     * NFS根路径
     */
    private String rootPath;

    /**
     * 基础URL，用于构建播放链接
     */
    private String baseUrl;
    
    /**
     * URL存储策略
     * RELATIVE: 存储相对路径（默认）
     * ABSOLUTE: 存储完整URL
     */
    private UrlStorageStrategy urlStorageStrategy = UrlStorageStrategy.RELATIVE;
    
    public enum UrlStorageStrategy {
        RELATIVE,  // 存储相对路径，运行时拼接
        ABSOLUTE   // 存储完整URL
    }
}
