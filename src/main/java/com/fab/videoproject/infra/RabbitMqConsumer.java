package com.fab.videoproject.infra;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Placeholder MQ consumer.
 */
@Component
public class RabbitMqConsumer {

    @RabbitListener(queues = "${rabbitmq.video-task-queue}")
    public void onMessage(String message) {
        // TODO handle incoming MQ messages
    }
}

