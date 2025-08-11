package com.fab.video_convert_platform.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC配置
 * 注册日志拦截器和其他Web相关配置
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    
    @Autowired
    private WebLogInterceptor webLogInterceptor;
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册日志拦截器
        registry.addInterceptor(webLogInterceptor)
                .addPathPatterns("/**")  // 拦截所有路径
                .excludePathPatterns(   // 排除不需要日志的路径
                    "/actuator/**",     // 健康检查
                    "/favicon.ico",     // 图标
                    "/error",           // 错误页面
                    "/static/**",       // 静态资源
                    "/css/**",          // CSS资源
                    "/js/**",           // JS资源
                    "/images/**"        // 图片资源
                );
    }
}
