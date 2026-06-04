package com.example.video_convert_platform.infra.log;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * 日志切面，用于记录Controller和Service层方法的入参、返回值和执行时间
 * 提供用户友好的日志格式和性能监控
 */
@Slf4j
@Aspect
@Component
public class LoggingAspect {

    @SuppressWarnings("EI_EXPOSE_REP2") // 依赖注入场景，预期行为
    private final ObjectMapper mapper;

    // 配置参数，可在application.yml中配置
    @Value("${logging.aspect.max-arg-length:500}")
    private int maxArgLength;

    @Value("${logging.aspect.max-result-length:500}")
    private int maxResultLength;

    @Value("${logging.aspect.slow-method-threshold:3000}")
    private long slowMethodThreshold;

    @Value("${logging.aspect.enabled:true}")
    private boolean loggingEnabled;

    public LoggingAspect(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Pointcut("execution(* com.example.video_convert_platform.interfaces.rest..*(..))")
    public void controllerMethods() {
    }

    @Pointcut("execution(* com.example.video_convert_platform.service..*(..))")
    public void serviceMethods() {
    }

    @Around("controllerMethods() || serviceMethods()")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!loggingEnabled) {
            return joinPoint.proceed();
        }

        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        String fullMethodName = className + "." + methodName + "()";

        // 格式化参数
        String formattedArgs = formatArguments(joinPoint.getArgs());

        long start = System.currentTimeMillis();

        log.info(" [{}] 开始执行 | 参数: {}", fullMethodName, formattedArgs);

        try {
            Object result = joinPoint.proceed();
            long cost = System.currentTimeMillis() - start;

            // 格式化返回值
            String formattedResult = formatResult(result);

            // 性能检查
            if (cost > slowMethodThreshold) {
                log.warn("⚠️ [{}] 执行完成 | 耗时: {}ms (超过阈值{}ms) | 返回: {}",
                    fullMethodName, cost, slowMethodThreshold, formattedResult);
            } else {
                log.info("✅ [{}] 执行完成 | 耗时: {}ms | 返回: {}",
                    fullMethodName, cost, formattedResult);
            }

            return result;
        } catch (Throwable ex) {
            long cost = System.currentTimeMillis() - start;

            // 根据异常类型使用不同的日志级别
            if (ex instanceof IllegalArgumentException || ex instanceof IllegalStateException) {
                log.warn("❌ [{}] 执行异常 | 耗时: {}ms | 异常类型: {} | 错误信息: {}",
                    fullMethodName, cost, ex.getClass().getSimpleName(), ex.getMessage());
            } else {
                log.error(" [{}] 执行异常 | 耗时: {}ms | 异常类型: {} | 错误信息: {}",
                    fullMethodName, cost, ex.getClass().getSimpleName(), ex.getMessage(), ex);
            }

            throw ex;
        }
    }

    /**
     * 格式化方法参数
     */
    private String formatArguments(Object[] args) {
        if (args == null || args.length == 0) {
            return "无参数";
        }

        try {
            String argsJson = toJson(args);
            return truncateString(argsJson, maxArgLength);
        } catch (Exception e) {
            return "参数解析失败: " + Arrays.toString(args);
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
            String resultJson = toJson(result);
            return truncateString(resultJson, maxResultLength);
        } catch (Exception e) {
            return "返回值解析失败: " + result.getClass().getSimpleName();
        }
    }

    /**
     * 截断过长的字符串
     */
    private String truncateString(String str, int maxLength) {
        if (str == null) {
            return "null";
        }

        if (str.length() <= maxLength) {
            return str;
        }

        return str.substring(0, maxLength) + "...(已截断,总长度:" + str.length() + ")";
    }

    /**
     * 将对象转换为JSON字符串
     */
    private String toJson(Object obj) {
        try {
            return mapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            if (obj instanceof Object[]) {
                return Arrays.toString((Object[]) obj);
            }
            return String.valueOf(obj);
        }
    }
}
