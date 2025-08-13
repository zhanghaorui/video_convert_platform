package com.fab.video_convert_platform.service.impl;

import com.fab.video_convert_platform.domain.ProjectConfig;
import com.fab.video_convert_platform.domain.VideoUploadTaskView;
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

    @SuppressWarnings("EI_EXPOSE_REP2") // 依赖注入场景，预期行为
    private final ITaskLogService taskLogService;
    @SuppressWarnings("EI_EXPOSE_REP2") // 依赖注入场景，预期行为
    private final IProjectService projectService;
    @SuppressWarnings("EI_EXPOSE_REP2") // 依赖注入场景，预期行为
    private final CallbackInfrastructure callbackInfrastructure;

    public CallbackServiceImpl(ITaskLogService taskLogService,
                               IProjectService projectService,
                               CallbackInfrastructure callbackInfrastructure) {
        this.taskLogService = taskLogService;
        this.projectService = projectService;
        this.callbackInfrastructure = callbackInfrastructure;
    }

    @Override
    public void notify(VideoUploadTaskView taskView) {
        // 检查任务来源，只对HTTP来源的任务进行回调
        if (taskView.isMqSource()) {
            taskLogService.info(taskView.getId(), "MQ来源任务，跳过HTTP回调");
            return;
        }

        // 获取项目配置
        ProjectConfig config = projectService.getByProjectNo(taskView.getProjectNo());
        if (config == null) {
            taskLogService.error(taskView.getId(), "Project config not found for callback");
            return;
        }
        
        // 检查是否配置了回调地址
        if (!config.hasCallbackUrl()) {
            taskLogService.info(taskView.getId(), "No callback url configured");
            return;
        }
        
        // 根据任务状态发送不同的回调
        if (taskView.isFinished()) {
            callbackInfrastructure.notifyTaskCompletion(taskView, config.getCallbackUrl());
        } else if (taskView.isFailed()) {
            callbackInfrastructure.notifyTaskFailure(taskView, config.getCallbackUrl(), taskView.getErrorMsg());
        } else {
            taskLogService.error(taskView.getId(), "Task status not suitable for callback: " + taskView.getStatus());
        }
    }
}
