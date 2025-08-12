package com.fab.video_convert_platform.config;

import com.fab.video_convert_platform.util.BusinessLogUtil;
import com.fab.video_convert_platform.util.LogTraceUtil;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Web请求日志拦截器
 * 自动为每个请求生成TraceId并记录访问日志
 * @author zhanghaorui
 */
@Component
public class WebLogInterceptor implements HandlerInterceptor {
    
    /**
     * TraceId请求头名称
     */
    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    
    /**
     * 请求开始时间属性名
     */
    private static final String START_TIME_ATTR = "startTime";
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 记录请求开始时间
        request.setAttribute(START_TIME_ATTR, System.currentTimeMillis());
        
        // 获取或生成TraceId
        String traceId = request.getHeader(TRACE_ID_HEADER);
        if (!StringUtils.hasText(traceId)) {
            traceId = LogTraceUtil.generateTraceId();
        }
        
        // 验证和清理TraceId防止HTTP响应分割攻击
        traceId = sanitizeTraceId(traceId);
        LogTraceUtil.setTraceId(traceId);
        
        // 安全地设置响应头中的TraceId
        if (isValidTraceId(traceId)) {
            response.setHeader(TRACE_ID_HEADER, traceId);
        }
        
        return true;
    }
    
    /**
     * 验证TraceId格式是否合法
     */
    private boolean isValidTraceId(String traceId) {
        return traceId != null && traceId.matches("^[a-zA-Z0-9\\-_]{1,128}$");
    }
    
    /**
     * 清理TraceId中的危险字符防止HTTP响应分割
     */
    private String sanitizeTraceId(String traceId) {
        if (traceId == null) {
            return null;
        }
        // 移除换行符等危险字符
        return traceId.replaceAll("[\\r\\n\\f]", "").trim();
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        try {
            // 计算响应时间
            Long startTime = (Long) request.getAttribute(START_TIME_ATTR);
            long responseTime = startTime != null ? System.currentTimeMillis() - startTime : 0;
            
            // 记录访问日志
            String params = getRequestParams(request);
            BusinessLogUtil.logApiAccess(
                request.getMethod(),
                request.getRequestURI(),
                params,
                responseTime,
                response.getStatus()
            );
            
            // 如果有异常，记录错误日志
            if (ex != null) {
                BusinessLogUtil.logBusinessError("WEB_REQUEST", 
                    "Request processing failed", ex, 
                    "URI=" + request.getRequestURI(), 
                    "Method=" + request.getMethod());
            }
        } finally {
            // 清理MDC
            LogTraceUtil.clearTraceId();
        }
    }
    
    /**
     * 获取请求参数字符串
     * 
     * @param request HTTP请求
     * @return 参数字符串
     */
    private String getRequestParams(HttpServletRequest request) {
        StringBuilder params = new StringBuilder();
        request.getParameterMap().forEach((key, values) -> {
            if (params.length() > 0) {
                params.append("&");
            }
            params.append(key).append("=");
            if (values != null && values.length > 0) {
                // 对于敏感参数，进行脱敏处理
                if (isSensitiveParam(key)) {
                    params.append("***");
                } else {
                    params.append(values[0]);
                }
            }
        });
        return params.toString();
    }
    
    /**
     * 判断是否为敏感参数
     * 
     * @param paramName 参数名
     * @return 是否敏感
     */
    private boolean isSensitiveParam(String paramName) {
        if (paramName == null) {
            return false;
        }
        String lowerName = paramName.toLowerCase();
        return lowerName.contains("password") 
            || lowerName.contains("token") 
            || lowerName.contains("secret")
            || lowerName.contains("key");
    }
}
