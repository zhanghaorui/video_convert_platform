package com.fab.videoproject.infra;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fab.videoproject.infra.MqVideoMessage;
import com.fab.videoproject.service.IVideoService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Placeholder MQ consumer.
 */
@Component
public class RabbitMqConsumer {

    private static final Logger log = LoggerFactory.getLogger(RabbitMqConsumer.class);

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private IVideoService videoService;

    @RabbitListener(queues = "${rabbitmq.video-task-queue}")
    public void onMessage(String message) {
        try {
            MqVideoMessage msg = objectMapper.readValue(message, MqVideoMessage.class);
            videoService.processMqMessage(msg);
        } catch (Exception e) {
            log.error("Failed to process MQ message", e);
        }
    }
}

