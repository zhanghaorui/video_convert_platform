package com.example.video_convert_platform;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling; // added

/**
 * 视频转换平台主应用类
 */
@SpringBootApplication
@EnableScheduling // 开启定时任务调度，用于孤立文件清理等后台维护任务
@MapperScan("com.example.video_convert_platform.mapper")
public class VideoConvertApplication {
    public static void main(String[] args) {
        SpringApplication.run(VideoConvertApplication.class, args);
    }
}
