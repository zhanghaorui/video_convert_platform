package com.fab.video_convert_platform.infra.infrastructure;

import com.fab.video_convert_platform.domain.VideoUploadTask;
import com.fab.video_convert_platform.domain.infrastructure.CallbackInfrastructure;
import com.fab.video_convert_platform.service.ITaskLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
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
    private final Tracer tracer;

    public RestCallbackInfrastructure(RestTemplate restTemplate, ITaskLogService taskLogService, Tracer tracer) {
        this.restTemplate = restTemplate;
        this.taskLogService = taskLogService;
        this.tracer = tracer;
    }

    @Override
    public void notifyTaskCompletion(VideoUploadTask task, String callbackUrl) {
        Map<String, Object> body = buildCallbackBody(task);
        body.put("status", "COMPLETED");

        Span span = tracer.nextSpan().name("http_callback").start();
        span.tag("task_id", String.valueOf(task.getId()));
        span.tag("url", callbackUrl);
        try (Tracer.SpanInScope ws = tracer.withSpan(span)) {
            ResponseEntity<Void> resp = restTemplate.postForEntity(callbackUrl, body, Void.class);
            span.tag("http_status", String.valueOf(resp.getStatusCodeValue()));
            taskLogService.info(task.getId(), "Task completion callback sent successfully");
            log.info("Task completion callback sent: taskId={}, url={}", task.getId(), callbackUrl);
        } catch (RestClientException e) {
            if (e instanceof HttpStatusCodeException) {
                span.tag("http_status", String.valueOf(((HttpStatusCodeException) e).getRawStatusCode()));
            }
            span.error(e);
            String errorMsg = "Task completion callback failed: " + e.getMessage();
            taskLogService.error(task.getId(), errorMsg);
            log.error("Task completion callback failed: taskId={}, url={}, error={}",
                task.getId(), callbackUrl, e.getMessage(), e);
        } finally {
            span.end();
        }
    }

    @Override
    public void notifyTaskFailure(VideoUploadTask task, String callbackUrl, String errorMessage) {
        Map<String, Object> body = buildCallbackBody(task);
        body.put("status", "FAILED");
        body.put("errorMessage", errorMessage);

        Span span = tracer.nextSpan().name("http_callback").start();
        span.tag("task_id", String.valueOf(task.getId()));
        span.tag("url", callbackUrl);
        try (Tracer.SpanInScope ws = tracer.withSpan(span)) {
            ResponseEntity<Void> resp = restTemplate.postForEntity(callbackUrl, body, Void.class);
            span.tag("http_status", String.valueOf(resp.getStatusCodeValue()));
            taskLogService.info(task.getId(), "Task failure callback sent successfully");
            log.info("Task failure callback sent: taskId={}, url={}", task.getId(), callbackUrl);
        } catch (RestClientException e) {
            if (e instanceof HttpStatusCodeException) {
                span.tag("http_status", String.valueOf(((HttpStatusCodeException) e).getRawStatusCode()));
            }
            span.error(e);
            String errorMsg = "Task failure callback failed: " + e.getMessage();
            taskLogService.error(task.getId(), errorMsg);
            log.error("Task failure callback failed: taskId={}, url={}, error={}",
                task.getId(), callbackUrl, e.getMessage(), e);
        } finally {
            span.end();
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
