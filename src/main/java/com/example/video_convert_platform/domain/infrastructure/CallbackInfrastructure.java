package com.example.video_convert_platform.domain.infrastructure;

import com.example.video_convert_platform.domain.VideoUploadTaskView;

/**
 * 回调服务基础设施接口
 * 定义向业务系统发送回调通知的抽象
 */
public interface CallbackInfrastructure {

    /**
     * 向业务系统发送任务完成通知
     * @param taskView 视频上传任务视图
     * @param callbackUrl 回调地址
     */
    void notifyTaskCompletion(VideoUploadTaskView taskView, String callbackUrl);

    /**
     * 向业务系统发送任务失败通知
     * @param taskView 视频上传任务视图
     * @param callbackUrl 回调地址
     * @param errorMessage 错误信息
     */
    void notifyTaskFailure(VideoUploadTaskView taskView, String callbackUrl, String errorMessage);
}
