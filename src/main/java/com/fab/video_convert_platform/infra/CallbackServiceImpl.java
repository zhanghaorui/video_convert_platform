package com.fab.video_convert_platform.infra;

import com.fab.video_convert_platform.domain.ProjectConfig;
import com.fab.video_convert_platform.domain.VideoUploadTask;
import com.fab.video_convert_platform.service.ICallbackService;
import com.fab.video_convert_platform.service.IProjectService;
import com.fab.video_convert_platform.service.ITaskLogService;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Dummy callback service that simply records a log entry.
 */
@Component
public class CallbackServiceImpl implements ICallbackService {

    private final ITaskLogService taskLogService;
    private final RestTemplate restTemplate;
    private final IProjectService projectService;

    public CallbackServiceImpl(ITaskLogService taskLogService,
                               RestTemplate restTemplate,
                               IProjectService projectService) {
        this.taskLogService = taskLogService;
        this.restTemplate = restTemplate;
        this.projectService = projectService;
    }

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
        try {
            restTemplate.postForEntity(config.getCallbackUrl(), body, Void.class);
            taskLogService.info(task.getId(), "callback to business system success");
        } catch (RestClientException e) {
            taskLogService.error(task.getId(), "callback to business system failed: " + e.getMessage());
        }
    }
}

