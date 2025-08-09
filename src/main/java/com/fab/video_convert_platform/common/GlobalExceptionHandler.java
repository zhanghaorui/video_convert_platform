package com.fab.video_convert_platform.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 * Handles application exceptions and converts them to ApiResponse.
 * @author 张浩锐
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ApiResponse<?> handleBusiness(BusinessException ex) {
        log.error("Business exception code={} msg={}", ex.getCode(), ex.getMessage(), ex);
        return ApiResponse.failure(ex.getErrorCode(), ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<?> handleException(Exception ex) {
        log.error("System exception", ex);
        return ApiResponse.failure(ErrorCode.SYSTEM_ERROR, ex.getMessage());
    }
}
