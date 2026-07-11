package com.example.video_convert_platform.service.listener;

import com.example.video_convert_platform.domain.event.TaskLogEvent;
import com.example.video_convert_platform.service.TaskLogService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Handles {@link TaskLogEvent} by delegating to {@link TaskLogService}.
 */
@Component
public class TaskLogEventListener {

    private final TaskLogService taskLogService;

    public TaskLogEventListener(TaskLogService taskLogService) {
        this.taskLogService = taskLogService;
    }

    @EventListener
    public void onTaskLog(TaskLogEvent event) {
        if (event.getLevel() == TaskLogEvent.Level.INFO) {
            taskLogService.info(event.getTaskId(), event.getMessage());
        } else {
            taskLogService.error(event.getTaskId(), event.getMessage());
        }
    }
}
