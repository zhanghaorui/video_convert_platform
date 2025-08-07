package com.fab.videoproject.controller;

import com.fab.videoproject.common.ApiResponse;
import com.fab.videoproject.service.IVideoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.beans.factory.annotation.Autowired;

@RestController
public class VideoController {

    @Autowired
    private IVideoService videoService;

    @GetMapping("/ping")
    public ApiResponse<String> ping() {
        return ApiResponse.success(videoService.ping());
    }
}
