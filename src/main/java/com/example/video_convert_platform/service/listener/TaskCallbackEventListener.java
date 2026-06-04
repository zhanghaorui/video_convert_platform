package com.example.video_convert_platform.service.listener;

import com.example.video_convert_platform.domain.event.TaskCallbackEvent;
import com.example.video_convert_platform.service.ICallbackService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Triggers business callbacks after task completion.
 */
@Slf4j
@Component
public class TaskCallbackEventListener {

    private final ICallbackService callbackService;

    public TaskCallbackEventListener(ICallbackService callbackService) {
        this.callbackService = callbackService;
    }

    @EventListener
    public void onCallback(TaskCallbackEvent event) {
        try {
            // event.getTask() 现在返回 VideoUploadTaskView，与接口 notify(VideoUploadTaskView) 匹配
            callbackService.notify(event.getTask());
        } catch (RuntimeException e) {
            log.error("业务回调失败", e);
        }
    }
}
