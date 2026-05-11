package com.fab.video_convert_platform.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 消息队列相关配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "mq")
public class MqProperties {

    /**
     * 队列配置
     */
    private Queues queues = new Queues();

    // 防御性 getter/setter 方法
    public Queues getQueues() {
        return queues != null ? new Queues(queues) : null;
    }

    public void setQueues(Queues queues) {
        this.queues = queues != null ? new Queues(queues) : null;
    }

    @Data
    public static class Queues {
        /**
         * 视频任务队列名
         */
        private String videoTask = "video.task.queue";

        // 拷贝构造器
        public Queues() {}

        public Queues(Queues other) {
            if (other != null) {
                this.videoTask = other.videoTask;
            }
        }
    }
}

