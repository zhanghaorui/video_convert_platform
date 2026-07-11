package com.example.video_convert_platform.service.impl;

import com.example.video_convert_platform.domain.ProjectConfig;
import com.example.video_convert_platform.domain.VideoUploadTaskView;
import com.example.video_convert_platform.domain.infrastructure.CallbackInfrastructure;
import com.example.video_convert_platform.config.BusinessProperties;
import com.example.video_convert_platform.service.CallbackService;
import com.example.video_convert_platform.service.ProjectService;
import com.example.video_convert_platform.service.TaskLogService;
import org.springframework.stereotype.Service;

/**
 * 回调服务实现
 * 应用服务层，负责编排回调业务流程
 */
@Service
public class CallbackServiceImpl implements CallbackService {

    @SuppressWarnings("EI_EXPOSE_REP2") // 依赖注入场景，预期行为
    private final TaskLogService taskLogService;
    @SuppressWarnings("EI_EXPOSE_REP2") // 依赖注入场景，预期行为
    private final ProjectService projectService;
    @SuppressWarnings("EI_EXPOSE_REP2") // 依赖注入场景，预期行为
    private final CallbackInfrastructure callbackInfrastructure;
    @SuppressWarnings("EI_EXPOSE_REP2") // 依赖注入场景，预期行为
    private final BusinessProperties businessProperties;

    public CallbackServiceImpl(TaskLogService taskLogService,
                               ProjectService projectService,
                               CallbackInfrastructure callbackInfrastructure,
                               BusinessProperties businessProperties) {
        this.taskLogService = taskLogService;
        this.projectService = projectService;
        this.callbackInfrastructure = callbackInfrastructure;
        this.businessProperties = businessProperties;
    }

    @Override
    public void notify(VideoUploadTaskView taskView) {
        BusinessProperties.Callback callback = businessProperties.getCallback();
        if (callback == null || !Boolean.TRUE.equals(callback.getEnabled())) {
            taskLogService.info(taskView.getId(), "Optional webhook callback disabled");
            return;
        }

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
        
        // 只有成功时才回调
        if (taskView.isFinished()) {
            callbackInfrastructure.notifyTaskCompletion(taskView, config.getCallbackUrl());
        } else if (taskView.isFailed()) {
            taskLogService.info(taskView.getId(), "Task failed, skip callback notification");
        } else {
            taskLogService.error(taskView.getId(), "Task status not suitable for callback: " + taskView.getStatus());
        }
    }
}
