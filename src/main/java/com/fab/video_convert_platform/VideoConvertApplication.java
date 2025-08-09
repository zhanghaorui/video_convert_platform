package com.fab.video_convert_platform;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 视频转换平台主应用类
 * @author 张浩锐
 */
@SpringBootApplication
@MapperScan("com.fab.video_convert_platform.mapper")
public class VideoConvertApplication {
    public static void main(String[] args) {
        SpringApplication.run(VideoConvertApplication.class, args);
    }
}
