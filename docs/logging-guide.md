# Logback 日志系统使用指南

## 日志系统概述

本项目使用 Logback 作为日志框架，提供了完整的日志解决方案，包括：

- **多环境配置**: 开发、测试、生产环境的不同日志策略
- **文件分类**: 按日志级别和业务类型分类存储
- **TraceId追踪**: 分布式请求链路追踪
- **自动轮转**: 日志文件按大小和时间自动轮转
- **异步写入**: 高性能异步日志记录

## 日志文件结构

```
logs/
├── app.log              # 应用主日志(INFO及以上)
├── error.log            # 错误日志(ERROR及以上)
├── business.log         # 业务日志(特殊业务事件)
├── access.log           # 访问日志(HTTP请求)
├── sql.log              # SQL执行日志
└── archives/            # 历史日志归档
    ├── app.2024-01-01.0.gz
    └── error.2024-01-01.0.gz
```

## 日志配置

### 1. 环境配置
在 `application.yml` 中配置不同环境的日志级别：

```yaml
spring:
  profiles:
    active: dev  # dev/test/prod

logging:
  aspect:
    enabled: true              # 启用方法执行日志
    slow-method-threshold: 3000 # 慢方法阈值(ms)
    log-args: true             # 记录方法参数
    log-result: false          # 记录方法返回值
```

### 2. 日志级别
- **开发环境(dev)**: DEBUG级别，详细日志输出
- **测试环境(test)**: INFO级别，适中的日志输出
- **生产环境(prod)**: WARN级别，只记录警告和错误

## 使用方式

### 1. 基础日志使用

```java
@RestController
public class ExampleController {
    private static final Logger logger = LoggerFactory.getLogger(ExampleController.class);
    
    @GetMapping("/example")
    public String example() {
        logger.info("处理example请求");
        logger.warn("这是一个警告");
        logger.error("发生错误", new RuntimeException("示例异常"));
        return "success";
    }
}
```

### 2. TraceId 使用

```java
// 自动生成TraceId (推荐)
String traceId = LogTraceUtil.getOrGenerateTraceId();
logger.info("当前TraceId: {}", traceId);

// 手动设置TraceId
LogTraceUtil.setTraceId("custom-trace-id");

// 在指定TraceId上下文中执行
LogTraceUtil.runWithTraceId(() -> {
    // 在这个代码块中，所有日志都会带上TraceId
    logger.info("这条日志会自动包含TraceId");
});
```

### 3. 业务日志使用

```java
// 视频上传日志
BusinessLogUtil.logVideoUpload(userId, projectName, fileName, fileType);

// 视频处理日志
BusinessLogUtil.logVideoProcess(userId, fileName, status, details);

// 回调日志
BusinessLogUtil.logCallback(callbackUrl, method, requestBody, responseCode, responseBody);

// 文件操作日志
BusinessLogUtil.logFileOperation(userId, operation, filePath, success, details);

// API访问日志
BusinessLogUtil.logApiAccess(url, method, status, duration, responseCode);
```

### 4. 自动切面日志

系统会自动记录以下内容：
- **Controller层**: 所有HTTP请求的处理日志
- **Service层**: 业务逻辑的执行日志
- **慢方法**: 超过阈值的方法会标记为慢方法

## 测试日志功能

启动应用后，可以通过以下API测试日志功能：

```bash
# 基础日志测试
curl http://localhost:8080/api/test/log/basic

# TraceId测试
curl http://localhost:8080/api/test/log/trace

# 业务日志测试
curl -X POST http://localhost:8080/api/test/log/business \
  -H "Content-Type: application/json" \
  -d '{"userId": "123", "fileName": "test.mp4"}'

# 慢方法测试
curl http://localhost:8080/api/test/log/slow

# 异常日志测试
curl "http://localhost:8080/api/test/log/exception?throwException=true"

# TraceId传递测试
curl http://localhost:8080/api/test/log/trace-propagation
```

## 日志模式说明

### 控制台日志格式
```
2024-01-15 10:30:45.123 [http-nio-8080-exec-1] INFO  [abc123] c.f.v.controller.VideoController - 开始处理视频上传请求
```

### 文件日志格式
```
2024-01-15 10:30:45.123 INFO  [abc123] [VideoController.uploadVideo:45] - 开始处理视频上传请求, args=[...]
```

格式说明：
- `[abc123]`: TraceId，用于追踪请求链路
- `VideoController.uploadVideo:45`: 类名.方法名:行号
- 自动包含线程信息、日志级别、时间戳

## 监控和维护

### 1. 日志文件轮转
- 单个文件最大: 50MB
- 历史文件保留: 30天
- 自动压缩存档

### 2. 性能优化
- 使用异步appender避免阻塞
- SQL日志仅在开发环境启用
- 大对象参数自动截断

### 3. 问题排查
1. 通过TraceId追踪完整请求链路
2. 查看error.log定位错误信息
3. 查看business.log了解业务流程
4. 查看access.log分析请求模式

## 注意事项

1. **生产环境**: 避免在循环中打印大量日志
2. **敏感信息**: 密码等敏感信息会自动过滤
3. **性能影响**: 方法参数日志在生产环境建议关闭
4. **磁盘空间**: 定期检查日志文件大小，配置合适的保留策略

## 配置定制

如需修改日志配置，编辑 `src/main/resources/logback-spring.xml` 文件：
- 调整日志级别
- 修改文件路径
- 变更轮转策略
- 自定义日志格式
