package com.fab.videoproject.controller;

import com.fab.videoproject.service.IVideoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing video operations.
 */
@RestController
@RequestMapping("/videos")
public class VideoController {

    @Autowired
    private IVideoService videoService;
    // endpoints for video handling will be added here
}
