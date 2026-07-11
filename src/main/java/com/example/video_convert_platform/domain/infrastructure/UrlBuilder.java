package com.example.video_convert_platform.domain.infrastructure;

/**
 * URL构建器接口
 * 领域层用于生成播放URL的抽象
 */
public interface UrlBuilder {

    /**
     * 构建完整播放URL
     *
     * @param relativePath 相对路径
     * @return 完整URL
     */
    String buildAbsoluteUrl(String relativePath);

    /**
     * 获取URL存储策略
     *
     * @return URL存储策略
     */
    UrlStorageStrategy getUrlStorageStrategy();

    /**
     * URL存储策略枚举
     */
    enum UrlStorageStrategy {
        /** 相对路径存储 */
        RELATIVE,
        /** 绝对路径存储 */
        ABSOLUTE
    }
}