package com.fab.video_convert_platform.infra;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fab.video_convert_platform.service.IVideoService;
import com.fab.video_convert_platform.service.dto.MqVideoMessage;
import com.rabbitmq.client.Channel;
import java.io.IOException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
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

    @RabbitListener(queues = "${rabbitmq.video-task-queue}", containerFactory = "manualAckContainerFactory")
    public void onMessage(Message message, Channel channel) throws IOException {
        long tag = message.getMessageProperties().getDeliveryTag();
        try {
            MqVideoMessage msg = objectMapper.readValue(message.getBody(), MqVideoMessage.class);
            if (!StringUtils.hasText(msg.getProjectNo()) ||
                !StringUtils.hasText(msg.getFilePath()) ||
                !StringUtils.hasText(msg.getFileMd5())) {
                log.error("Missing required fields in MQ message: {}", msg);
                channel.basicReject(tag, false);
                return;
            }
            videoService.processMqMessage(msg);
            channel.basicAck(tag, false);
        } catch (Exception e) {
            log.error("Failed to process MQ message", e);
            channel.basicNack(tag, false, true);
        }
    }
}
