package com.fab.video_convert_platform.infra.monitor.rabbitmq;

public class RabbitMQMonitorException extends RuntimeException {

    public RabbitMQMonitorException(String message) {
        super(message);
    }

    public RabbitMQMonitorException(String message, Throwable cause) {
        super(message, cause);
    }
}
