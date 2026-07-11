package com.example.video_convert_platform.infra.infrastructure;

import com.example.video_convert_platform.config.NfsProperties;
import com.example.video_convert_platform.domain.infrastructure.UrlBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * URL构建器实现
 * 基于NFS配置生成播放URL
 */
@Component
public class DefaultUrlBuilder implements UrlBuilder {

    private final NfsProperties nfsProperties;

    public DefaultUrlBuilder(NfsProperties nfsProperties) {
        this.nfsProperties = nfsProperties;
    }

    @Override
    public String buildAbsoluteUrl(String relativePath) {
        String baseUrl = nfsProperties.getBaseUrl();
        if (!StringUtils.hasText(baseUrl)) {
            return relativePath;
        }

        String cleanBaseUrl = baseUrl.replaceAll("/$", "");
        String cleanRelativePath = relativePath.startsWith("/") ? relativePath : "/" + relativePath;
        return cleanBaseUrl + cleanRelativePath;
    }

    @Override
    public UrlStorageStrategy getUrlStorageStrategy() {
        if (nfsProperties.getUrlStorageStrategy() == NfsProperties.UrlStorageStrategy.ABSOLUTE) {
            return UrlStorageStrategy.ABSOLUTE;
        }
        return UrlStorageStrategy.RELATIVE;
    }
}