package com.fab.video_convert_platform.infra.monitor.rabbitmq;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RabbitMQAlertService {

    private final RabbitMQMonitorConfig config;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public RabbitMQAlertService(RabbitMQMonitorConfig config) {
        this.config = config;
    }

    public void sendAlert(String title, String message, RabbitMQMonitorService.AlertLevel level) {
        if (!config.getAlert().isEnabled()) {
            log.warn("告警功能已禁用: {} - {}", title, message);
            return;
        }

        String alertMessage = formatAlertMessage(title, message, level);
        
        if (config.getAlert().getDingtalkWebhook() != null) {
            sendToDingtalk(alertMessage, level);
        }
        
        if (config.getAlert().getWechatWebhook() != null) {
            sendToWechat(alertMessage, level);
        }
    }

    private String formatAlertMessage(String title, String message, 
                                      RabbitMQMonitorService.AlertLevel level) {
        String timestamp = java.time.LocalDateTime.now().toString();
        
        return String.format(
            "[%s] %s\n%s\n时间: %s",
            level.name(), title, message, timestamp
        );
    }

    private void sendToDingtalk(String message, RabbitMQMonitorService.AlertLevel level) {
        try {
            String webhookUrl = config.getAlert().getDingtalkWebhook();
            if (webhookUrl == null || webhookUrl.isEmpty()) {
                log.warn("钉钉机器人Webhook未配置");
                return;
            }

            Map<String, Object> payload = new HashMap<>();
            payload.put("msgtype", "text");
            
            Map<String, String> text = new HashMap<>();
            text.put("content", message);
            payload.put("text", text);
            
            String jsonPayload = objectMapper.writeValueAsString(payload);
            
            postToWebhook(webhookUrl, jsonPayload);
            
            log.info("钉钉告警发送成功: {}", message);
        } catch (Exception e) {
            log.error("钉钉告警发送失败: {}", e.getMessage(), e);
        }
    }

    private void sendToWechat(String message, RabbitMQMonitorService.AlertLevel level) {
        try {
            String webhookUrl = config.getAlert().getWechatWebhook();
            if (webhookUrl == null || webhookUrl.isEmpty()) {
                log.warn("企微机器人Webhook未配置");
                return;
            }

            Map<String, Object> payload = new HashMap<>();
            payload.put("msgtype", "text");
            
            Map<String, String> text = new HashMap<>();
            text.put("content", message);
            payload.put("text", text);
            
            String jsonPayload = objectMapper.writeValueAsString(payload);
            
            postToWebhook(webhookUrl, jsonPayload);
            
            log.info("企微告警发送成功: {}", message);
        } catch (Exception e) {
            log.error("企微告警发送失败: {}", e.getMessage(), e);
        }
    }

    private void postToWebhook(String webhookUrl, String jsonPayload) throws IOException {
        URL url = new URL(webhookUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setDoOutput(true);
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        
        conn.getOutputStream().write(jsonPayload.getBytes("UTF-8"));
        
        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            log.error("Webhook响应异常: {}", responseCode);
        }
        
        conn.disconnect();
    }
}
