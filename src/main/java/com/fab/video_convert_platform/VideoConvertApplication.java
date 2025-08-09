package com.fab.video_convert_platform;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.fab.video_convert_platform.mapper")
public class VideoConvertApplication {
    public static void main(String[] args) {
        SpringApplication.run(VideoConvertApplication.class, args);
    }
}
