package com.fab.video_convert_platform.infra.monitor.rabbitmq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RabbitMQMonitorService {

    private final RabbitMQMonitorConfig config;
    private final ConnectionFactory rabbitConnectionFactory;
    private final RabbitMQMetricsRecorder metricsRecorder;
    private final RabbitMQAlertService alertService;
    
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    private volatile boolean connected = false;
    private volatile long lastCheckTime = 0;

    @Autowired
    public RabbitMQMonitorService(RabbitMQMonitorConfig config, 
                                   ConnectionFactory rabbitConnectionFactory,
                                   RabbitMQMetricsRecorder metricsRecorder,
                                   RabbitMQAlertService alertService) {
        this.config = config;
        this.rabbitConnectionFactory = rabbitConnectionFactory;
        this.metricsRecorder = metricsRecorder;
        this.alertService = alertService;
    }

    public void start() {
        if (!config.isEnabled()) {
            log.warn("RabbitMQ监控已禁用");
            return;
        }

        scheduler.scheduleAtFixedRate(this::checkHealth, 0, 
            config.getMonitoringIntervalSeconds(), TimeUnit.SECONDS);
        
        log.info("RabbitMQ监控服务已启动，监控间隔: {}秒", config.getMonitoringIntervalSeconds());
    }

    public void stop() {
        scheduler.shutdown();
        log.info("RabbitMQ监控服务已停止");
    }

    private void checkHealth() {
        try {
            long startTime = System.currentTimeMillis();
            
            checkConnectionStatus();
            checkQueueDepth();
            checkConsumerCount();
            
            lastCheckTime = System.currentTimeMillis();
            connected = true;
            
            if (log.isDebugEnabled()) {
                log.debug("RabbitMQ健康检查完成，耗时: {}ms", System.currentTimeMillis() - startTime);
            }
        } catch (Exception e) {
            connected = false;
            log.error("RabbitMQ健康检查失败: {}", e.getMessage(), e);
            
            if (alertService != null) {
                alertService.sendAlert("RabbitMQ连接异常", 
                    "RabbitMQ服务不可用: " + e.getMessage(), 
                    AlertLevel.CRITICAL);
            }
        }
    }

    private void checkConnectionStatus() throws IOException {
        try (Channel channel = rabbitConnectionFactory.createConnection().createChannel()) {
            channel.isOpen();
            connected = true;
        }
    }

    private void checkQueueDepth() throws IOException {
        try (Channel channel = rabbitConnectionFactory.createConnection().createChannel()) {
            String queueName = config.getQueues().getVideoTask();
            
            AMQP.Queue.DeclareOk declareOk = channel.queueDeclarePassive(queueName);
            long messageCount = declareOk.getMessageCount();
            int consumerCount = declareOk.getConsumerCount();
            
            metricsRecorder.recordQueueDepth(messageCount);
            metricsRecorder.recordConsumerCount(consumerCount);
            
            log.debug("队列[{}]深度: {}, 消费者数: {}", queueName, messageCount, consumerCount);
            
            checkQueueAlerts(queueName, messageCount, consumerCount);
        }
    }

    private void checkConsumerCount() {
        int consumerCount = metricsRecorder.getLastConsumerCount().get();
        if (consumerCount < config.getAlert().getConsumerWarning()) {
            log.warn("消费者数量异常: {}", consumerCount);
        }
    }

    private void checkQueueAlerts(String queueName, long messageCount, int consumerCount) {
        AlertLevel alertLevel = null;
        String alertMessage = null;

        if (consumerCount == 0) {
            alertLevel = AlertLevel.CRITICAL;
            alertMessage = String.format("队列[%s]无消费者连接！深度: %d", queueName, messageCount);
        } else if (messageCount > config.getAlert().getQueueDepthCritical()) {
            alertLevel = AlertLevel.CRITICAL;
            alertMessage = String.format("队列[%s]深度严重积压: %d（阈值: %d）", 
                queueName, messageCount, config.getAlert().getQueueDepthCritical());
        } else if (messageCount > config.getAlert().getQueueDepthWarning()) {
            alertLevel = AlertLevel.WARNING;
            alertMessage = String.format("队列[%s]深度积压: %d（阈值: %d）", 
                queueName, messageCount, config.getAlert().getQueueDepthWarning());
        }

        if (alertLevel != null && alertService != null) {
            alertService.sendAlert("RabbitMQ队列异常", alertMessage, alertLevel);
        }
    }

    public boolean isConnected() {
        return connected && (System.currentTimeMillis() - lastCheckTime) < 
            (config.getMonitoringIntervalSeconds() * 2 * 1000);
    }

    public RabbitMQMetricsSnapshot getMetricsSnapshot() {
        return new RabbitMQMetricsSnapshot(
            connected,
            metricsRecorder.getLastQueueDepth().get(),
            metricsRecorder.getLastConsumerCount().get(),
            metricsRecorder.getMessageSuccessRate(),
            metricsRecorder.getMessageRetryCount().get()
        );
    }

    public enum AlertLevel {
        INFO, WARNING, CRITICAL
    }
}
