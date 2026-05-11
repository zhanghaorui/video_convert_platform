package com.fab.video_convert_platform.infra.monitor.rabbitmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.IOException;
import java.util.concurrent.TimeoutException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Slf4j
@Data
@Component
@ConfigurationProperties(prefix = "rabbitmq.monitor")
public class RabbitMQMonitorConfig {

    private boolean enabled = true;
    private String host = "172.18.14.228";
    private int port = 5672;
    private String username = "admin";
    private String password = "admin";
    private String virtualHost = "/video";
    private int monitoringIntervalSeconds = 30;
    private int timeoutSeconds = 10;

    private AlertConfig alert = new AlertConfig();

    public static class AlertConfig {
        private boolean enabled = true;
        private String dingtalkWebhook;
        private String wechatWebhook;
        private String email;
        private int queueDepthWarning = 1000;
        private int queueDepthCritical = 5000;
        private int consumerWarning = 1;
        private double successRateWarning = 0.95;
        private int retryCountWarning = 100;
        private int retryCountCritical = 500;
    }

    public Connection createConnection() {
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(host);
            factory.setPort(port);
            factory.setUsername(username);
            factory.setPassword(password);
            factory.setVirtualHost(virtualHost);
            factory.setConnectionTimeout(timeoutSeconds * 1000);
            return factory.newConnection();
        } catch (IOException | TimeoutException e) {
            log.error("创建RabbitMQ连接失败: {}", e.getMessage(), e);
            throw new RabbitMQMonitorException("创建RabbitMQ连接失败", e);
        }
    }

    public Channel createChannel() {
        Connection connection = createConnection();
        try {
            return connection.createChannel();
        } catch (IOException e) {
            log.error("创建RabbitMQ Channel失败: {}", e.getMessage(), e);
            throw new RabbitMQMonitorException("创建RabbitMQ Chainel失败", e);
        }
    }
}
