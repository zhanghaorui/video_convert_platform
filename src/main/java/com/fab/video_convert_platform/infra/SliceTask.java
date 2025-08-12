package com.fab.video_convert_platform.infra;

import com.fab.video_convert_platform.domain.ProjectConfig;
import com.fab.video_convert_platform.domain.VideoUploadTask;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Container for slicing task data pulled from local queue.
 */
public class SliceTask {
    private final ProjectConfig projectConfig;
    private final VideoUploadTask uploadTask;

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Task data is managed externally")
    public SliceTask(ProjectConfig projectConfig, VideoUploadTask uploadTask) {
        this.projectConfig = projectConfig;
        this.uploadTask = uploadTask;
    }

    /**
     * 获取项目配置（防御性返回）
     * @return 项目配置对象
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Domain object is intentionally shared")
    public ProjectConfig getProjectConfig() {
        // 由于ProjectConfig是领域对象，理想情况下应该返回不可变视图
        return projectConfig; // TODO: 考虑实现ProjectConfig的不可变视图
    }

    /**
     * 获取上传任务（防御性返回）
     * @return 上传任务对象
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Domain object is intentionally shared")
    public VideoUploadTask getUploadTask() {
        // 由于VideoUploadTask是领域对象，理想情况下应该返回不可变视图
        return uploadTask; // TODO: 考虑实现VideoUploadTask的不可变视图
    }
}
