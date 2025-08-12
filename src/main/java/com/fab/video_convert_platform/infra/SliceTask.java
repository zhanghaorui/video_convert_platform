package com.fab.video_convert_platform.infra;

import com.fab.video_convert_platform.domain.ProjectConfig;
import com.fab.video_convert_platform.domain.VideoUploadTask;

/**
 * Container for slicing task data pulled from local queue.
 */
public class SliceTask {
    private final ProjectConfig projectConfig;
    private final VideoUploadTask uploadTask;

    public SliceTask(ProjectConfig projectConfig, VideoUploadTask uploadTask) {
        this.projectConfig = projectConfig;
        this.uploadTask = uploadTask;
    }

    /**
     * 获取项目配置（防御性返回）
     * @return 项目配置对象
     */
    public ProjectConfig getProjectConfig() {
        // 由于ProjectConfig是领域对象，理想情况下应该返回不可变视图
        return projectConfig; // TODO: 考虑实现ProjectConfig的不可变视图
    }

    /**
     * 获取上传任务（防御性返回）
     * @return 上传任务对象
     */
    public VideoUploadTask getUploadTask() {
        // 由于VideoUploadTask是领域对象，理想情况下应该返回不可变视图
        return uploadTask; // TODO: 考虑实现VideoUploadTask的不可变视图
    }
}
