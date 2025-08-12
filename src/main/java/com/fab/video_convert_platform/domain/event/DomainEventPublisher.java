package com.fab.video_convert_platform.domain.event;

/**
 * Publishes domain events to interested listeners.
 */
public interface DomainEventPublisher {
    void publish(DomainEvent event);
}
