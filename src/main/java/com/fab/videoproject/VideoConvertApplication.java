package com.fab.videoproject;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class VideoConvertApplication {
    public static void main(String[] args) {
        SpringApplication.run(VideoConvertApplication.class, args);
    }
}
