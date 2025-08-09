package com.fab.video_convert_platform.infra.infrastructure;

import com.fab.video_convert_platform.domain.VideoUploadTask;
import com.fab.video_convert_platform.domain.infrastructure.CallbackInfrastructure;
import com.fab.video_convert_platform.service.ITaskLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * 回调服务基础设施实现
 * 基于HTTP REST的回调通知实现
 */
@Slf4j
@Component
public class RestCallbackInfrastructure implements CallbackInfrastructure {

    private final RestTemplate restTemplate;
    private final ITaskLogService taskLogService;

    public RestCallbackInfrastructure(RestTemplate restTemplate, ITaskLogService taskLogService) {
        this.restTemplate = restTemplate;
        this.taskLogService = taskLogService;
    }

    @Override
    public void notifyTaskCompletion(VideoUploadTask task, String callbackUrl) {
        Map<String, Object> body = buildCallbackBody(task);
        body.put("status", "COMPLETED");
        
        try {
            restTemplate.postForEntity(callbackUrl, body, Void.class);
            taskLogService.info(task.getId(), "Task completion callback sent successfully");
            log.info("Task completion callback sent: taskId={}, url={}", task.getId(), callbackUrl);
        } catch (RestClientException e) {
            String errorMsg = "Task completion callback failed: " + e.getMessage();
            taskLogService.error(task.getId(), errorMsg);
            log.error("Task completion callback failed: taskId={}, url={}, error={}", 
                task.getId(), callbackUrl, e.getMessage(), e);
        }
    }

    @Override
    public void notifyTaskFailure(VideoUploadTask task, String callbackUrl, String errorMessage) {
        Map<String, Object> body = buildCallbackBody(task);
        body.put("status", "FAILED");
        body.put("errorMessage", errorMessage);
        
        try {
            restTemplate.postForEntity(callbackUrl, body, Void.class);
            taskLogService.info(task.getId(), "Task failure callback sent successfully");
            log.info("Task failure callback sent: taskId={}, url={}", task.getId(), callbackUrl);
        } catch (RestClientException e) {
            String errorMsg = "Task failure callback failed: " + e.getMessage();
            taskLogService.error(task.getId(), errorMsg);
            log.error("Task failure callback failed: taskId={}, url={}, error={}", 
                task.getId(), callbackUrl, e.getMessage(), e);
        }
    }

    /**
     * 构建回调请求体
     */
    private Map<String, Object> buildCallbackBody(VideoUploadTask task) {
        Map<String, Object> body = new HashMap<>();
        body.put("taskId", task.getId());
        body.put("uuid", task.getUuid());
        body.put("projectNo", task.getProjectNo());
        body.put("patientCode", task.getPatientCode());
        body.put("tpStage", task.getTpStage());
        body.put("versionNo", task.getVersionNo());
        return body;
    }
}
