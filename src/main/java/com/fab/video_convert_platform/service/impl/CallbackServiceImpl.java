package com.fab.video_convert_platform.service.impl;

import com.fab.video_convert_platform.domain.ProjectConfig;
import com.fab.video_convert_platform.domain.VideoUploadTask;
import com.fab.video_convert_platform.domain.infrastructure.CallbackInfrastructure;
import com.fab.video_convert_platform.service.ICallbackService;
import com.fab.video_convert_platform.service.IProjectService;
import com.fab.video_convert_platform.service.ITaskLogService;
import org.springframework.stereotype.Service;

/**
 * 回调服务实现
 * 应用服务层，负责编排回调业务流程
 */
@Service
public class CallbackServiceImpl implements ICallbackService {

    private final ITaskLogService taskLogService;
    private final IProjectService projectService;
    private final CallbackInfrastructure callbackInfrastructure;

    public CallbackServiceImpl(ITaskLogService taskLogService,
                               IProjectService projectService,
                               CallbackInfrastructure callbackInfrastructure) {
        this.taskLogService = taskLogService;
        this.projectService = projectService;
        this.callbackInfrastructure = callbackInfrastructure;
    }

    @Override
    public void notify(VideoUploadTask task) {
        // 获取项目配置
        ProjectConfig config = projectService.getByProjectNo(task.getProjectNo());
        if (config == null) {
            taskLogService.error(task.getId(), "Project config not found for callback");
            return;
        }
        
        // 检查是否配置了回调地址
        if (!config.hasCallbackUrl()) {
            taskLogService.info(task.getId(), "No callback url configured");
            return;
        }
        
        // 根据任务状态发送不同的回调
        if (task.isFinished()) {
            callbackInfrastructure.notifyTaskCompletion(task, config.getCallbackUrl());
        } else if (task.isFailed()) {
            callbackInfrastructure.notifyTaskFailure(task, config.getCallbackUrl(), task.getErrorMsg());
        } else {
            taskLogService.error(task.getId(), "Task status not suitable for callback: " + task.getStatus());
        }
    }
}
