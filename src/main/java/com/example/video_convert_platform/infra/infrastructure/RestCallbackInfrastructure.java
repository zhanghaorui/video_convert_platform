package com.example.video_convert_platform.infra.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.video_convert_platform.domain.VideoUploadTaskView;
import com.example.video_convert_platform.domain.infrastructure.CallbackInfrastructure;
import com.example.video_convert_platform.service.ITaskLogService;
import com.example.video_convert_platform.util.ArchivePathUtil;
import com.example.video_convert_platform.config.NfsProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * 回调服务基础设施实现
 * 基于HTTP REST的回调通知实现
 */
@Slf4j
@Component
public class RestCallbackInfrastructure implements CallbackInfrastructure {

    @SuppressWarnings("EI_EXPOSE_REP2") // 依赖注入场景，预期行为
    private final RestTemplate restTemplate;
    @SuppressWarnings("EI_EXPOSE_REP2") // 依赖注入场景，预期行为
    private final ITaskLogService taskLogService;
    @SuppressWarnings("EI_EXPOSE_REP2") // 依赖注入场景，预期行为
    private final Tracer tracer;
    @SuppressWarnings("EI_EXPOSE_REP2") // 依赖注入场景，预期行为
    private final NfsProperties nfsProperties;
    @SuppressWarnings("EI_EXPOSE_REP2") // 依赖注入场景，预期行为
    private final ObjectMapper objectMapper;

    public RestCallbackInfrastructure(RestTemplate restTemplate, ITaskLogService taskLogService, 
                                    Tracer tracer, NfsProperties nfsProperties, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.taskLogService = taskLogService;
        this.tracer = tracer;
        this.nfsProperties = nfsProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    public void notifyTaskCompletion(VideoUploadTaskView taskView, String callbackUrl) {
        // 构建视频播放地址
        String remotePath = ArchivePathUtil.buildPlayUrl(
            taskView.getProjectNo(),
            taskView.getPatientCode(), 
            taskView.getTpStage(),
            taskView.getVersionNo(),
            taskView.getUuid(),
            "normal"  // 使用标准分辨率版本
        );
        
        String playUrl = buildAbsoluteUrl(remotePath);
        
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("taskId", taskView.getId());
        body.add("projectNo", taskView.getProjectNo());
        body.add("patientCode", taskView.getPatientCode());
        body.add("tpStage", taskView.getTpStage());
        body.add("versionNo", taskView.getVersionNo());
        body.add("uuid", taskView.getUuid());
        body.add("playUrl", playUrl);

        log.info("Task callback play URL: {}", playUrl);
        log.info("==========Task callback start==========");
        log.info("Task callback request: {}", toJsonString(body));

        Span span = tracer.nextSpan().name("http_callback").start();
        span.tag("task_id", String.valueOf(taskView.getId()));
        span.tag("url", callbackUrl);
        try (Tracer.SpanInScope ws = tracer.withSpan(span)) {
            String callbackResponse = restTemplate.postForObject(callbackUrl, body, String.class);
            span.tag("http_status", "200");
            taskLogService.info(taskView.getId(), "Task completion callback sent successfully");
            log.info("Task callback response: {}", toJsonString(callbackResponse));
            log.info("Task completion callback sent: taskId={}, url={}", taskView.getId(), callbackUrl);
        } catch (RestClientException e) {
            span.error(e);
            String errorMsg = "Task callback failed: " + e.getMessage();
            taskLogService.error(taskView.getId(), errorMsg);
            log.info("Task callback failed: {}", e.getMessage());
        } finally {
            span.end();
            log.info("==========Task callback end==========");
        }
    }

    @Override
    public void notifyTaskFailure(VideoUploadTaskView taskView, String callbackUrl, String errorMessage) {
        // 失败不回调，只记录日志
        taskLogService.info(taskView.getId(), "Task failed, skip callback notification");
        log.info("Task failed, skip callback: taskId={}, error={}", taskView.getId(), errorMessage);
    }

    /**
     * 构建完整URL
     */
    private String buildAbsoluteUrl(String relativePath) {
        String baseUrl = nfsProperties.getBaseUrl();
        if (!StringUtils.hasText(baseUrl)) {
            return relativePath;
        }
        
        String cleanBaseUrl = baseUrl.replaceAll("/$", "");
        String cleanRelativePath = relativePath.startsWith("/") ? relativePath : "/" + relativePath;
        return cleanBaseUrl + cleanRelativePath;
    }
    
    /**
     * 将对象转换为JSON字符串
     */
    private String toJsonString(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return String.valueOf(obj);
        }
    }
}
