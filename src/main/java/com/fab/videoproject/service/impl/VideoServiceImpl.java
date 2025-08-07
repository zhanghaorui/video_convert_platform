package com.fab.videoproject.service.impl;

import com.fab.videoproject.service.IVideoService;
import org.springframework.stereotype.Service;

/**
 * Implementation of video service.
 */
@Service
public class VideoServiceImpl implements IVideoService {
    @Override
    public String ping() {
        return "video service pong";
    }
}
