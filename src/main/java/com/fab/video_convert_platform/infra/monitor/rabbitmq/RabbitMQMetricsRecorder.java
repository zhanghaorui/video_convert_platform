package com.fab.video_convert_platform.infra.monitor.rabbitmq;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RabbitMQMetricsRecorder {

    private final MeterRegistry meterRegistry;
    
    private final AtomicLong lastQueueDepth = new AtomicLong(0);
    private final AtomicInteger lastConsumerCount = new AtomicInteger(0);
    private final AtomicInteger messageRetryCount = new AtomicInteger(0);
    private final AtomicLong messageTotalCount = new AtomicLong(0);
    private final AtomicLong messageSuccessCount = new AtomicLong(0);
    
    private Counter successCounter;
    private Counter failureCounter;
    private Counter retryCounter;

    @Autowired
    public RabbitMQMetricsRecorder(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        initCounters();
    }

    private void initCounters() {
        successCounter = Counter.builder("rabbitmq.message.success")
            .description("成功处理的消息数量")
            .register(meterRegistry);
        
        failureCounter = Counter.builder("rabbitmq.message.failure")
            .description("处理失败的消息数量")
            .register(meterRegistry);
        
        retryCounter = Counter.builder("rabbitmq.message.retry")
            .description("消息重试次数")
            .register(meterRegistry);
    }

    public void recordMessageProcessed(boolean success) {
        messageTotalCount.incrementAndGet();
        if (success) {
            messageSuccessCount.incrementAndGet();
            successCounter.increment();
        } else {
            failureCounter.increment();
        }
    }

    public void recordMessageRetry() {
        messageRetryCount.incrementAndGet();
        retryCounter.increment();
    }

    public void recordQueueDepth(long depth) {
        lastQueueDepth.set(depth);
    }

    public void recordConsumerCount(int count) {
        lastConsumerCount.set(count);
    }

    public double getMessageSuccessRate() {
        long total = messageTotalCount.get();
        if (total == 0) {
            return 1.0;
        }
        return (double) messageSuccessCount.get() / total;
    }

    public AtomicLong getLastQueueDepth() {
        return lastQueueDepth;
    }

    public AtomicInteger getLastConsumerCount() {
        return lastConsumerCount;
    }

    public AtomicInteger getMessageRetryCount() {
        return messageRetryCount;
    }

    public void reset() {
        lastQueueDepth.set(0);
        lastConsumerCount.set(0);
        messageRetryCount.set(0);
        messageTotalCount.set(0);
        messageSuccessCount.set(0);
    }
    
    @Data
    public static class RabbitMQMetricsSnapshot {
        private boolean connected;
        private long queueDepth;
        private int consumerCount;
        private double successRate;
        private int retryCount;
        
        public RabbitMQMetricsSnapshot(boolean connected, long queueDepth, 
                                       int consumerCount, double successRate, int retryCount) {
            this.connected = connected;
            this.queueDepth = queueDepth;
            this.consumerCount = consumerCount;
            this.successRate = successRate;
            this.retryCount = retryCount;
        }
    }
}
