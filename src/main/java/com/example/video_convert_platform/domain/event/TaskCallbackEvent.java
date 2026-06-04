package com.example.video_convert_platform.domain.event;

import com.example.video_convert_platform.domain.VideoUploadTask;
import com.example.video_convert_platform.domain.VideoUploadTaskView;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Event indicating the task has finished processing and may trigger an optional webhook.
 */
public class TaskCallbackEvent implements DomainEvent {
    private final VideoUploadTaskView taskView;

    /**
     * 构造任务回调事件
     * @param task 任务对象
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Task object is managed externally")
    public TaskCallbackEvent(VideoUploadTask task) {
        if (task == null) {
            throw new IllegalArgumentException("Task cannot be null");
        }
        // 创建不可变视图，确保事件传递过程中任务数据不被修改
        this.taskView = VideoUploadTaskView.of(task);
    }

    /**
     * 获取任务不可变视图
     * @return 任务对象的不可变视图
     */
    public VideoUploadTaskView getTask() {
        return taskView;
    }
}
