package com.fab.video_convert_platform.service.impl;

import com.fab.video_convert_platform.domain.VideoUploadTask;
import com.fab.video_convert_platform.service.ICallbackService;
import com.fab.video_convert_platform.service.ITaskLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Dummy callback service that simply records a log entry.
 */
@Service
public class CallbackServiceImpl implements ICallbackService {

    @Autowired
    private ITaskLogService taskLogService;

    @Override
    public void notify(VideoUploadTask task) {
        // TODO: invoke external callback
        taskLogService.info(task.getId(), "callback to business system success");
    }
}

