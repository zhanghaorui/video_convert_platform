package com.fab.video_convert_platform.domain.event;

import com.fab.video_convert_platform.domain.VideoUploadTask;
import com.fab.video_convert_platform.domain.VideoUploadTaskView;

/**
 * Event indicating the task has finished processing and should trigger callback.
 * @author zhanghaorui
 */
public class TaskCallbackEvent implements DomainEvent {
    private final VideoUploadTaskView taskView;

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
