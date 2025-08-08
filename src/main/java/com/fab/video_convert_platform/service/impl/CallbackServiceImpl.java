package com.fab.video_convert_platform.service.impl;

import com.fab.video_convert_platform.domain.ProjectConfig;
import com.fab.video_convert_platform.domain.VideoUploadTask;
import com.fab.video_convert_platform.service.ICallbackService;
import com.fab.video_convert_platform.service.IProjectService;
import com.fab.video_convert_platform.service.ITaskLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Dummy callback service that simply records a log entry.
 */
@Service
public class CallbackServiceImpl implements ICallbackService {

    @Autowired
    private ITaskLogService taskLogService;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private IProjectService projectService;

    @Override
    public void notify(VideoUploadTask task) {
        ProjectConfig config = projectService.getByProjectNo(task.getProjectNo());
        if (config == null || config.getCallbackUrl() == null || config.getCallbackUrl().isEmpty()) {
            taskLogService.info(task.getId(), "no callback url configured");
            return;
        }
        Map<String, Object> body = new HashMap<>();
        body.put("taskId", task.getId());
        body.put("projectNo", task.getProjectNo());
        body.put("status", task.getStatus());
        restTemplate.postForEntity(config.getCallbackUrl(), body, Void.class);
        taskLogService.info(task.getId(), "callback to business system success");
    }
}

