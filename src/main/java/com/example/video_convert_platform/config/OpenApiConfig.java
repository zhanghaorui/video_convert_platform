package com.example.video_convert_platform.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI配置类
 * 配置API文档的基本信息
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("视频归档平台 API")
                .version("1.0.0")
                .description("医疗视频归档与转码平台 RESTful API 文档\n\n" +
                    "## 主要功能\n" +
                    "- 视频文件上传（完整上传/分片上传）\n" +
                    "- 视频转码与HLS切片\n" +
                    "- NFS归档存储\n" +
                    "- 播放URL查询\n" +
                    "- 项目配置管理\n")
                .contact(new Contact()
                    .name("开发团队")
                    .email("support@example.com")));
    }
}