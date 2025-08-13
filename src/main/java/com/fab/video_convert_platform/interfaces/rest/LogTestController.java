package com.fab.video_convert_platform.interfaces.rest;

import com.fab.video_convert_platform.common.ApiResponse;
import com.fab.video_convert_platform.util.BusinessLogUtil;
import com.fab.video_convert_platform.util.LogTraceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 日志测试控制器
 * 用于验证日志系统功能
 */
@RestController
@RequestMapping("/api/test/log")
public class LogTestController {
    
    private static final Logger logger = LoggerFactory.getLogger(LogTestController.class);
    
    /**
     * 测试基础日志功能
     */
    @GetMapping("/basic")
    public ApiResponse<String> testBasicLog() {
        logger.info("基础日志测试开始");
        logger.warn("这是一个警告日志");
        logger.error("这是一个错误日志测试（非真实错误）");
        
        return ApiResponse.success("基础日志测试完成，请查看日志文件");
    }
    
    /**
     * 测试TraceId功能
     */
    @GetMapping("/trace")
    public ApiResponse<Map<String, String>> testTraceId() {
        String traceId = LogTraceUtil.getOrGenerateTraceId();
        logger.info("当前TraceId: {}", traceId);
        
        Map<String, String> result = new HashMap<>();
        result.put("traceId", traceId);
        result.put("message", "TraceId测试完成");
        
        return ApiResponse.success(result);
    }
    
    /**
     * 测试业务日志功能
     */
    @PostMapping("/business")
    public ApiResponse<String> testBusinessLog(@RequestBody Map<String, Object> request) {
        String userIdStr = (String) request.get("userId");
        Long userId = userIdStr != null ? Long.parseLong(userIdStr) : 1L;
        String fileName = (String) request.getOrDefault("fileName", "test.mp4");
        
        // 模拟视频上传日志
        BusinessLogUtil.logVideoUpload(userId, "test-project", fileName, "mp4");
        
        // 模拟视频处理日志
        BusinessLogUtil.logVideoProcess(userId, fileName, "PROCESSING", "进度50%");
        
        // 模拟API访问日志
        BusinessLogUtil.logApiAccess("/api/test/log/business", "POST", "200", 156L, 200);
        
        return ApiResponse.success("业务日志测试完成");
    }
    
    /**
     * 测试慢方法日志
     */
    @GetMapping("/slow")
    public ApiResponse<String> testSlowMethod() throws InterruptedException {
        logger.info("开始执行慢方法测试");
        
        // 模拟慢操作
        Thread.sleep(4000);
        
        logger.info("慢方法执行完成");
        return ApiResponse.success("慢方法测试完成，应该记录慢方法警告日志");
    }
    
    /**
     * 测试异常日志
     */
    @GetMapping("/exception")
    public ApiResponse<String> testExceptionLog(@RequestParam(defaultValue = "false") boolean throwException) {
        if (throwException) {
            logger.error("即将抛出测试异常");
            throw new RuntimeException("这是一个测试异常，用于验证异常日志记录");
        }
        
        return ApiResponse.success("异常日志测试完成（未抛出异常）");
    }
    
    /**
     * 测试TraceId传递
     */
    @GetMapping("/trace-propagation")
    public ApiResponse<String> testTracePropagation() {
        String traceId = LogTraceUtil.getOrGenerateTraceId();
        logger.info("开始TraceId传递测试，当前TraceId: {}", traceId);
        
        // 模拟调用其他方法
        simulateServiceCall();
        
        return ApiResponse.success("TraceId传递测试完成，TraceId: " + traceId);
    }
    
    private void simulateServiceCall() {
        logger.info("模拟Service层调用，TraceId应该自动传递");
        
        // 再次调用以验证TraceId传递
        simulateRepositoryCall();
    }
    
    private void simulateRepositoryCall() {
        logger.info("模拟Repository层调用，TraceId应该保持一致");
    }
}
