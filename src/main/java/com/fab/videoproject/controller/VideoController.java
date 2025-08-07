package com.fab.videoproject.controller;

import com.fab.videoproject.common.ApiResponse;
import com.fab.videoproject.service.IVideoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
public class VideoController {

    @Resource
    private IVideoService videoService;

    @GetMapping("/ping")
    public ApiResponse<String> ping() {
        return ApiResponse.success(videoService.ping());
    }
}
