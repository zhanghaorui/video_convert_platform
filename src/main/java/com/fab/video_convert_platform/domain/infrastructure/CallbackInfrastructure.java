package com.fab.video_convert_platform.domain.infrastructure;

import com.fab.video_convert_platform.domain.VideoUploadTask;

/**
 * 回调服务基础设施接口
 * 定义向业务系统发送回调通知的抽象
 */
public interface CallbackInfrastructure {

    /**
     * 向业务系统发送任务完成通知
     * @param task 视频上传任务
     * @param callbackUrl 回调地址
     */
    void notifyTaskCompletion(VideoUploadTask task, String callbackUrl);

    /**
     * 向业务系统发送任务失败通知
     * @param task 视频上传任务
     * @param callbackUrl 回调地址
     * @param errorMessage 错误信息
     */
    void notifyTaskFailure(VideoUploadTask task, String callbackUrl, String errorMessage);
}
