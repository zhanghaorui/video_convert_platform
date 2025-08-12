package com.fab.video_convert_platform.domain.event;

import com.fab.video_convert_platform.domain.VideoUploadTask;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Event indicating the task has finished processing and should trigger callback.
 */
@Getter
@AllArgsConstructor
public class TaskCallbackEvent implements DomainEvent {
    private final VideoUploadTask task;
}
