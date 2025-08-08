package com.fab.videoproject.infra.log;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Logging aspect capturing method args, return values and execution time
 * for controller and service layers.
 */
@Slf4j
@Aspect
@Component
public class LoggingAspect {

    private final ObjectMapper mapper;

    public LoggingAspect(ObjectMapper mapper) {
        this.mapper = mapper;
    }
    @Pointcut("execution(* com.fab.videoproject.controller..*(..))")
    public void controllerMethods() {
    }

    @Pointcut("execution(* com.fab.videoproject.service..*(..))")
    public void serviceMethods() {
    }

    @Around("controllerMethods() || serviceMethods()")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        String method = joinPoint.getSignature().toShortString();
        String args = toJson(joinPoint.getArgs());
        long start = System.currentTimeMillis();
        log.info("Enter {} args={}", method, args);
        try {
            Object result = joinPoint.proceed();
            long cost = System.currentTimeMillis() - start;
            log.info("Exit {} return={} cost={}ms", method, toJson(result), cost);
            return result;
        } catch (Throwable ex) {
            long cost = System.currentTimeMillis() - start;
            log.error("Exception in {} cost={}ms msg={}", method, cost, ex.getMessage(), ex);
            throw ex;
        }
    }

    private String toJson(Object obj) {
        try {
            return mapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return obj instanceof Object[] ? Arrays.toString((Object[]) obj) : String.valueOf(obj);
        }
    }
}

