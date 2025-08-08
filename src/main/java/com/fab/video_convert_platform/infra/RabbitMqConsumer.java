package com.fab.video_convert_platform.infra;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fab.video_convert_platform.service.dto.MqVideoMessage;
import com.fab.video_convert_platform.service.IVideoService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

/**
 * Placeholder MQ consumer.
 */
@Slf4j
@Component
@Profile("!test")
public class RabbitMqConsumer {

    private final ObjectMapper objectMapper;
    private final IVideoService videoService;

    public RabbitMqConsumer(ObjectMapper objectMapper, IVideoService videoService) {
        this.objectMapper = objectMapper;
        this.videoService = videoService;
    }

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
