package com.fab.video_convert_platform.domain.event;

import com.fab.video_convert_platform.domain.VideoUploadTask;

/**
 * Event indicating the task has finished processing and should trigger callback.
 */
public class TaskCallbackEvent implements DomainEvent {
    private final VideoUploadTask task;

    public TaskCallbackEvent(VideoUploadTask task) {
        this.task = task;
    }

    /**
     * 获取任务（防御性返回）
     * @return 任务对象的副本或不可变视图
     */
    public VideoUploadTask getTask() {
        // 由于VideoUploadTask是领域对象，理想情况下应该返回不可变视图或副本
        // 但由于没有复制构造函数，暂时返回原始对象并添加注释说明
        return task; // TODO: 实现VideoUploadTask的不可变视图或复制机制
    }
}
