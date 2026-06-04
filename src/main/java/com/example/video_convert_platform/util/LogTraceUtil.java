package com.example.video_convert_platform.util;

import org.slf4j.MDC;
import java.util.UUID;

/**
 * 日志追踪工具类
 * 用于生成和管理TraceId，方便问题追踪
 */
public class LogTraceUtil {
    
    /**
     * TraceId在MDC中的键名
     */
    public static final String TRACE_ID = "traceId";
    
    /**
     * 生成新的TraceId
     * 
     * @return 新的TraceId
     */
    public static String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
    
    /**
     * 设置TraceId到MDC
     * 
     * @param traceId 追踪ID
     */
    public static void setTraceId(String traceId) {
        MDC.put(TRACE_ID, traceId);
    }
    
    /**
     * 获取当前TraceId
     * 
     * @return 当前TraceId，如果不存在则返回null
     */
    public static String getTraceId() {
        return MDC.get(TRACE_ID);
    }
    
    /**
     * 获取当前TraceId，如果不存在则生成新的
     * 
     * @return TraceId
     */
    public static String getOrGenerateTraceId() {
        String traceId = getTraceId();
        if (traceId == null) {
            traceId = generateTraceId();
            setTraceId(traceId);
        }
        return traceId;
    }
    
    /**
     * 清除当前线程的TraceId
     */
    public static void clearTraceId() {
        MDC.remove(TRACE_ID);
    }
    
    /**
     * 清除当前线程的所有MDC数据
     */
    public static void clearMDC() {
        MDC.clear();
    }
    
    /**
     * 执行业务代码并自动管理TraceId
     * 
     * @param runnable 业务代码
     */
    public static void runWithTraceId(Runnable runnable) {
        String originalTraceId = getTraceId();
        try {
            if (originalTraceId == null) {
                setTraceId(generateTraceId());
            }
            runnable.run();
        } finally {
            if (originalTraceId == null) {
                clearTraceId();
            }
        }
    }
    
    /**
     * 执行业务代码并使用指定的TraceId
     * 
     * @param traceId 指定的TraceId
     * @param runnable 业务代码
     */
    public static void runWithTraceId(String traceId, Runnable runnable) {
        String originalTraceId = getTraceId();
        try {
            setTraceId(traceId);
            runnable.run();
        } finally {
            if (originalTraceId != null) {
                setTraceId(originalTraceId);
            } else {
                clearTraceId();
            }
        }
    }
}
