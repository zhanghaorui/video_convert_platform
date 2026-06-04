package com.example.video_convert_platform.common;

import com.example.video_convert_platform.config.LoggingProperties;
import com.example.video_convert_platform.util.LogTraceUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 方法执行日志切面
 * 记录Service层和Controller层方法的执行时间和参数
 */
@Aspect
@Component
public class MethodLogAspect {
    
    private static final Logger logger = LoggerFactory.getLogger(MethodLogAspect.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Autowired
    private LoggingProperties loggingProperties;
    
    /**
     * 切入Service层所有方法
     */
    @Around("execution(* com.example.video_convert_platform.service..*.*(..))")
    public Object logServiceMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!loggingProperties.getAspect().isEnabled()) {
            return joinPoint.proceed();
        }
        
        return logMethodExecution(joinPoint, "SERVICE");
    }
    
    /**
     * 切入Controller层所有方法
     */
    @Around("execution(* com.example.video_convert_platform.interfaces.rest..*.*(..))")
    public Object logControllerMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!loggingProperties.getAspect().isEnabled()) {
            return joinPoint.proceed();
        }
        
        return logMethodExecution(joinPoint, "CONTROLLER");
    }
    
    private Object logMethodExecution(ProceedingJoinPoint joinPoint, String layer) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        String traceId = LogTraceUtil.getOrGenerateTraceId();
        
        long startTime = System.currentTimeMillis();
        
        // 记录方法开始执行
        if (loggingProperties.getAspect().isLogArgs()) {
            Object[] args = filterSensitiveArgs(joinPoint.getArgs());
            logger.info("[{}] {}#{} 开始执行, traceId={}, args={}", 
                layer, className, methodName, traceId, formatArgs(args));
        } else {
            logger.info("[{}] {}#{} 开始执行, traceId={}", 
                layer, className, methodName, traceId);
        }
        
        Object result = null;
        Throwable exception = null;
        
        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable e) {
            exception = e;
            throw e;
        } finally {
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            
            if (exception != null) {
                // 记录异常
                logger.error("[{}] {}#{} 执行异常, traceId={}, duration={}ms, error={}", 
                    layer, className, methodName, traceId, duration, exception.getMessage());
            } else {
                // 记录正常结束
                if (duration > loggingProperties.getAspect().getSlowMethodThreshold()) {
                    logger.warn("[{}] {}#{} 执行完成[慢方法], traceId={}, duration={}ms", 
                        layer, className, methodName, traceId, duration);
                } else {
                    String logMsg = "[{}] {}#{} 执行完成, traceId={}, duration={}ms";
                    if (loggingProperties.getAspect().isLogResult() && result != null) {
                        logger.info(logMsg + ", result={}", 
                            layer, className, methodName, traceId, duration, formatResult(result));
                    } else {
                        logger.info(logMsg, layer, className, methodName, traceId, duration);
                    }
                }
            }
        }
    }
    
    /**
     * 过滤敏感参数
     */
    private Object[] filterSensitiveArgs(Object[] args) {
        if (args == null) {
            return null;
        }
        
        Object[] filteredArgs = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof HttpServletRequest) {
                filteredArgs[i] = "[HttpServletRequest]";
            } else if (args[i] instanceof HttpServletResponse) {
                filteredArgs[i] = "[HttpServletResponse]";
            } else {
                filteredArgs[i] = args[i];
            }
        }
        return filteredArgs;
    }
    
    /**
     * 格式化参数
     */
    private String formatArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return "[]";
        }
        
        try {
            return objectMapper.writeValueAsString(args);
        } catch (JsonProcessingException e) {
            return "[序列化失败: " + e.getMessage() + "]";
        }
    }
    
    /**
     * 格式化返回值
     */
    private String formatResult(Object result) {
        if (result == null) {
            return "null";
        }
        
        try {
            // 如果返回值过大，只记录类型
            String json = objectMapper.writeValueAsString(result);
            if (json.length() > 500) {
                return "[" + result.getClass().getSimpleName() + " - 内容过长已省略]";
            }
            return json;
        } catch (JsonProcessingException e) {
            return "[" + result.getClass().getSimpleName() + " - 序列化失败]";
        }
    }
}
