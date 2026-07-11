package com.example.video_convert_platform.infra.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.video_convert_platform.domain.VideoUploadTaskView;
import com.example.video_convert_platform.domain.infrastructure.CallbackInfrastructure;
import com.example.video_convert_platform.service.TaskLogService;
import com.example.video_convert_platform.util.ArchivePathUtil;
import com.example.video_convert_platform.config.NfsProperties;
import com.example.video_convert_platform.config.BusinessProperties;
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
    private final TaskLogService taskLogService;
    @SuppressWarnings("EI_EXPOSE_REP2") // 依赖注入场景，预期行为
    private final Tracer tracer;
    @SuppressWarnings("EI_EXPOSE_REP2") // 依赖注入场景，预期行为
    private final NfsProperties nfsProperties;
    @SuppressWarnings("EI_EXPOSE_REP2") // 依赖注入场景，预期行为
    private final ObjectMapper objectMapper;
    @SuppressWarnings("EI_EXPOSE_REP2") // 依赖注入场景，预期行为
    private final BusinessProperties businessProperties;

    public RestCallbackInfrastructure(RestTemplate restTemplate, TaskLogService taskLogService,
                                    Tracer tracer, NfsProperties nfsProperties, ObjectMapper objectMapper,
                                    BusinessProperties businessProperties) {
        this.restTemplate = restTemplate;
        this.taskLogService = taskLogService;
        this.tracer = tracer;
        this.nfsProperties = nfsProperties;
        this.objectMapper = objectMapper;
        this.businessProperties = businessProperties;
    }

    @Override
    public void notifyTaskCompletion(VideoUploadTaskView taskView, String callbackUrl) {
        // 校验回调URL白名单
        if (!isCallbackUrlAllowed(callbackUrl)) {
            taskLogService.error(taskView.getId(), "回调URL不在白名单中: " + callbackUrl);
            log.warn("Callback URL not in whitelist: taskId={}, url={}", taskView.getId(), callbackUrl);
            return;
        }

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

    /**
     * 校验回调URL是否在白名单中
     *
     * @param callbackUrl 回调URL
     * @return 是否允许
     */
    private boolean isCallbackUrlAllowed(String callbackUrl) {
        // 如果未启用白名单校验，则允许所有URL
        BusinessProperties.Callback callback = businessProperties.getCallback();
        if (callback == null || !Boolean.TRUE.equals(callback.getEnableUrlWhitelist())) {
            return true;
        }

        // 白名单为空时，拒绝所有URL
        java.util.List<String> allowedDomains = callback.getAllowedDomains();
        if (allowedDomains == null || allowedDomains.isEmpty()) {
            log.warn("URL whitelist enabled but no domains configured");
            return false;
        }

        // 解析URL获取域名
        try {
            java.net.URI uri = java.net.URI.create(callbackUrl);
            String host = uri.getHost();
            if (host == null) {
                log.warn("Invalid callback URL: {}", callbackUrl);
                return false;
            }

            // 检查域名是否在白名单中
            for (String allowed : allowedDomains) {
                if (isDomainMatch(host, allowed)) {
                    return true;
                }
            }

            return false;
        } catch (Exception e) {
            log.warn("Failed to parse callback URL: {}", callbackUrl);
            return false;
        }
    }

    /**
     * 检查域名是否匹配白名单规则
     * 支持通配符格式，如 *.example.com
     *
     * @param host 实际域名
     * @param pattern 白名单规则
     * @return 是否匹配
     */
    private boolean isDomainMatch(String host, String pattern) {
        if (pattern.startsWith("*.")) {
            // 通配符匹配：*.example.com 匹配 sub.example.com
            String domain = pattern.substring(2);
            return host.equals(domain) || host.endsWith("." + domain);
        }
        return host.equals(pattern);
    }
}
