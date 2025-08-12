package com.fab.video_convert_platform.domain.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Domain event representing task log messages.
 */
@Getter
@AllArgsConstructor
public class TaskLogEvent implements DomainEvent {
    private final Long taskId;
    private final Level level;
    private final String message;

    public enum Level {
        INFO, ERROR
    }
}
