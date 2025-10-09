package com.fab.video_convert_platform.infra;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fab.video_convert_platform.service.IVideoService;
import com.fab.video_convert_platform.service.dto.MqVideoMessage;
import com.rabbitmq.client.Channel;
import java.io.IOException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * Placeholder MQ consumer.
 */
@Slf4j
@Component
@ConditionalOnProperty(value = "mq.enabled", havingValue = "true", matchIfMissing = false)
public class RabbitMqConsumer {

    @SuppressWarnings("EI_EXPOSE_REP2") // 依赖注入场景，预期行为
    private final ObjectMapper objectMapper;
    @SuppressWarnings("EI_EXPOSE_REP2") // 依赖注入场景，预期行为
    private final IVideoService videoService;

    public RabbitMqConsumer(ObjectMapper objectMapper, IVideoService videoService) {
        this.objectMapper = objectMapper;
        this.videoService = videoService;
    }

    // 使用内存缓存来跟踪消息重试次数
    private final java.util.concurrent.ConcurrentHashMap<String, Integer> retryCountMap = 
        new java.util.concurrent.ConcurrentHashMap<>();

    @RabbitListener(queues = "${mq.queues.video-task}", containerFactory = "manualAckContainerFactory")
    public void onMessage(Message message, Channel channel) throws IOException {
        long tag = message.getMessageProperties().getDeliveryTag();
        
        // 使用消息体的MD5作为唯一标识来追踪重试次数
        String messageId = generateMessageId(message);
        int retryCount = retryCountMap.getOrDefault(messageId, 0);
        
        // 最大重试次数
        final int MAX_RETRY_COUNT = 3;

        try {
            MqVideoMessage msg = objectMapper.readValue(message.getBody(), MqVideoMessage.class);
            if (!StringUtils.hasText(msg.getProjectNo()) ||
                !StringUtils.hasText(msg.getFilePath()) ||
                !StringUtils.hasText(msg.getFileMd5())) {
                log.error("Missing required fields in MQ message: {}", msg);
                retryCountMap.remove(messageId); // 清理缓存
                channel.basicReject(tag, false);
                return;
            }
            videoService.processMqMessage(msg);
            retryCountMap.remove(messageId); // 处理成功，清理缓存
            channel.basicAck(tag, false);
        } catch (Exception e) {
            String filePath = extractFilePath(message);
            
            // 检查是否超过最大重试次数
            if (retryCount >= MAX_RETRY_COUNT) {
                log.error("消息处理失败超过最大重试次数({})，丢弃消息. 文件路径: {}, 错误: {}", 
                    MAX_RETRY_COUNT, filePath, e.getMessage());
                retryCountMap.remove(messageId); // 清理缓存
                // 拒绝消息且不重新排队，直接丢弃
                channel.basicReject(tag, false);
            } else {
                log.warn("消息处理失败(第{}次尝试)，将重试. 文件路径: {}, 错误: {}", 
                    retryCount + 1, filePath, e.getMessage());
                
                // 增加重试计数
                retryCountMap.put(messageId, retryCount + 1);
                
                // 重新排队重试
                channel.basicNack(tag, false, true);
            }
        }
    }
    
    private String generateMessageId(Message message) {
        try {
            // 使用消息体生成唯一ID
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(message.getBody());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            // 降级方案：使用消息体内容哈希
            return "msg_" + message.getBody().hashCode();
        }
    }
    
    private String extractFilePath(Message message) {
        try {
            MqVideoMessage msg = objectMapper.readValue(message.getBody(), MqVideoMessage.class);
            return msg.getFilePath();
        } catch (Exception e) {
            return "unknown";
        }
    }
}
