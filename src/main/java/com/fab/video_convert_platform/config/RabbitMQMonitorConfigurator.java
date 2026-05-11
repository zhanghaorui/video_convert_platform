package com.fab.video_convert_platform.config;

import com.fab.video_convert_platform.infra.monitor.rabbitmq.RabbitMQAlertService;
import com.fab.video_convert_platform.infra.monitor.rabbitmq.RabbitMQMonitorConfig;
import com.fab.video_convert_platform.infra.monitor.rabbitmq.RabbitMQMonitorService;
import com.fab.video_convert_platform.infra.monitor.rabbitmq.RabbitMQMetricsRecorder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(RabbitMQMonitorConfig.class)
@ConditionalOnProperty(prefix = "rabbitmq.monitor", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RabbitMQMonitorConfigurator {

    @Bean
    public RabbitMQMetricsRecorder rabbitMQMetricsRecorder() {
        return new RabbitMQMetricsRecorder();
    }

    @Bean
    public RabbitMQMonitorService rabbitMQMonitorService(
            RabbitMQMonitorConfig config,
            com.rabbitmq.client.ConnectionFactory rabbitConnectionFactory,
            RabbitMQMetricsRecorder metricsRecorder,
            RabbitMQAlertService alertService) {
        RabbitMQMonitorService service = new RabbitMQMonitorService(
            config, rabbitConnectionFactory, metricsRecorder, alertService);
        service.start();
        return service;
    }

    @Bean
    public RabbitMQAlertService rabbitMQAlertService(RabbitMQMonitorConfig config) {
        return new RabbitMQAlertService(config);
    }
}
