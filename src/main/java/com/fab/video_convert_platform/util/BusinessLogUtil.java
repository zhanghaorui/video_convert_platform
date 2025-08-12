package com.fab.video_convert_platform.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 业务日志工具类
 * 提供结构化的业务日志记录功能
 */
public class BusinessLogUtil {
    
    /**
     * 业务日志Logger
     */
    private static final Logger BUSINESS_LOGGER = LoggerFactory.getLogger("BUSINESS");
    
    /**
     * 访问日志Logger
     */
    private static final Logger ACCESS_LOGGER = LoggerFactory.getLogger("ACCESS");
    
    /**
     * 记录业务操作日志
     * 
     * @param operation 操作名称
     * @param message 日志消息
     * @param params 操作参数
     */
    public static void logBusiness(String operation, String message, Object... params) {
        LogTraceUtil.getOrGenerateTraceId(); // 确保TraceId存在
        BUSINESS_LOGGER.info("[{}] {} - {}", operation, message, formatParams(params));
    }
    
    /**
     * 记录业务错误日志
     * 
     * @param operation 操作名称
     * @param message 错误消息
     * @param throwable 异常对象
     * @param params 操作参数
     */
    public static void logBusinessError(String operation, String message, Throwable throwable, Object... params) {
        LogTraceUtil.getOrGenerateTraceId(); // 确保TraceId存在
        String errorMsg = throwable != null ? throwable.getMessage() : "null";
        BUSINESS_LOGGER.error("[{}] {} - {} - Exception: {}", operation, message, formatParams(params), errorMsg, throwable);
    }
    
    /**
     * 记录视频上传业务日志
     * 
     * @param taskId 任务ID
     * @param projectNo 项目编号
     * @param operation 操作类型
     * @param message 日志消息
     */
    public static void logVideoUpload(Long taskId, String projectNo, String operation, String message) {
        logBusiness("VIDEO_UPLOAD", 
            String.format("TaskId=%s, ProjectNo=%s, Operation=%s, %s", taskId, projectNo, operation, message));
    }
    
    /**
     * 记录视频处理业务日志
     * 
     * @param taskId 任务ID
     * @param operation 操作类型
     * @param message 日志消息
     * @param params 额外参数
     */
    public static void logVideoProcess(Long taskId, String operation, String message, Object... params) {
        logBusiness("VIDEO_PROCESS", 
            String.format("TaskId=%s, Operation=%s, %s", taskId, operation, message), params);
    }
    
    /**
     * 记录回调业务日志
     * 
     * @param taskId 任务ID
     * @param callbackUrl 回调地址
     * @param status 回调状态
     * @param message 日志消息
     */
    public static void logCallback(Long taskId, String callbackUrl, String status, String message) {
        logBusiness("CALLBACK", 
            String.format("TaskId=%s, CallbackUrl=%s, Status=%s, %s", taskId, callbackUrl, status, message));
    }
    
    /**
     * 记录文件操作业务日志
     * 
     * @param operation 操作类型 (UPLOAD/DOWNLOAD/DELETE等)
     * @param filePath 文件路径
     * @param fileSize 文件大小
     * @param message 日志消息
     */
    public static void logFileOperation(String operation, String filePath, Long fileSize, String message) {
        logBusiness("FILE_OPERATION", 
            String.format("Operation=%s, FilePath=%s, FileSize=%s, %s", operation, filePath, fileSize, message));
    }
    
    /**
     * 记录API访问日志
     * 
     * @param method HTTP方法
     * @param uri 请求URI
     * @param params 请求参数
     * @param responseTime 响应时间(毫秒)
     * @param status 响应状态
     */
    public static void logApiAccess(String method, String uri, String params, long responseTime, int status) {
        String traceId = LogTraceUtil.getTraceId();
        ACCESS_LOGGER.info("API_ACCESS method={} uri={} params={} responseTime={}ms status={} traceId={}", 
            method, uri, params, responseTime, status, traceId);
    }
    
    /**
     * 记录MQ消息处理日志
     * 
     * @param queueName 队列名称
     * @param operation 操作类型
     * @param message 日志消息
     * @param params 消息参数
     */
    public static void logMqMessage(String queueName, String operation, String message, Object... params) {
        logBusiness("MQ_MESSAGE", 
            String.format("Queue=%s, Operation=%s, %s", queueName, operation, message), params);
    }
    
    /**
     * 记录系统启动关闭日志
     * 
     * @param event 事件类型 (STARTUP/SHUTDOWN)
     * @param message 日志消息
     */
    public static void logSystemEvent(String event, String message) {
        logBusiness("SYSTEM_EVENT", String.format("Event=%s, %s", event, message));
    }
    
    /**
     * 格式化参数为字符串
     * 
     * @param params 参数数组
     * @return 格式化后的字符串
     */
    private static String formatParams(Object... params) {
        if (params == null || params.length == 0) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("Params=[");
        for (int i = 0; i < params.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(params[i]);
        }
        sb.append("]");
        return sb.toString();
    }
}
