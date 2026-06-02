# 医学视频归档平台 (Video Convert Platform)

基于 Spring Boot 2.7 构建的医学影像视频转码与归档平台，支持大文件分片上传、FFmpeg 转码、HLS 切片归档及多维度监控。

---

## 目录

- [功能特性](#功能特性)
- [技术栈](#技术栈)
- [快速开始](#快速开始)
- [环境配置](#环境配置)
- [API 接口](#api-接口)
- [监控与运维](#监控与运维)
- [构建与测试](#构建与测试)
- [项目结构](#项目结构)

---

## 功能特性

- **视频上传**：支持整文件上传与大文件分片上传（断点续传）
- **视频转码**：调用 FFmpeg 将原始视频转为多画质（480p / 720p）MP4
- **HLS 切片**：转码完成后自动切片为 HLS（`.m3u8` + `.ts`），便于流媒体播放
- **归档管理**：按项目 / 受试者 / 访视点维度归档，支持 NFS 共享存储
- **MD5 校验**：上传完成后进行文件完整性校验
- **异步处理**：通过 RabbitMQ 解耦上传与转码流程
- **回调通知**：转码完成后支持 HTTP 回调，内置重试机制
- **分布式追踪**：集成 Spring Cloud Sleuth + Zipkin
- **可观测性**：Actuator 健康检查 + Micrometer + Prometheus 指标采集

---

## 技术栈

| 层次 | 技术 |
|------|------|
| 框架 | Spring Boot 2.7.18 |
| 持久层 | MyBatis-Plus 3.5.5 + MySQL 8.0 |
| 消息队列 | RabbitMQ（Spring AMQP） |
| 视频处理 | FFmpeg（本地调用） |
| 链路追踪 | Spring Cloud Sleuth + Zipkin |
| 监控 | Spring Actuator + Micrometer + Prometheus |
| 代码质量 | JaCoCo + SpotBugs + PMD |
| 测试 | JUnit 5 + Testcontainers |
| 构建工具 | Maven 3.x |
| Java 版本 | Java 8 |

---

## 快速开始

### 前置依赖

- Java 8+
- Maven 3.6+
- MySQL 8.0
- RabbitMQ 3.x
- FFmpeg（需在系统 PATH 中可访问）
- （可选）Zipkin、Prometheus

### 1. 克隆仓库

```bash
git clone https://github.com/zhanghaorui/video_convert_platform.git
cd video_convert_platform
```

### 2. 配置环境变量

```bash
cp .env.example .env
# 编辑 .env，填写数据库、RabbitMQ、FFmpeg 等配置
```

### 3. 初始化数据库

将对应的 SQL 脚本导入 MySQL，确保数据库名与 `.env` 中 `DB_NAME` 一致。

### 4. 启动应用

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

应用默认监听 `http://localhost:8080`。

---

## 环境配置

主要配置均通过环境变量注入，参考 [`.env.example`](.env.example)：

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `DB_HOST` | 数据库地址 | `localhost` |
| `DB_PORT` | 数据库端口 | `3306` |
| `DB_NAME` | 数据库名 | `video_convert_platform` |
| `DB_USERNAME` | 数据库用户名 | — |
| `DB_PASSWORD` | 数据库密码 | — |
| `RABBITMQ_HOST` | RabbitMQ 地址 | `localhost` |
| `RABBITMQ_PORT` | RabbitMQ 端口 | `5672` |
| `VIDEO_TEMP_DIR` | 视频临时目录 | `/tmp/video-processing` |
| `VIDEO_ARCHIVE_ROOT` | 归档根目录 | `/data/video-archive` |
| `FFMPEG_THREADS` | FFmpeg 并发线程数 | `2` |
| `ZIPKIN_BASE_URL` | Zipkin 地址 | `http://localhost:9411` |

多环境配置文件：

| Profile | 文件 | 说明 |
|---------|------|------|
| `dev` | `application-dev.yml` | 开发环境，端口 8080 |
| `test` | `application-test.yml` | 测试环境，端口 8081 |
| `prod` | `application-prod.yml` | 生产环境 |

---

## API 接口

所有接口统一响应格式：

```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

### 视频管理 `/api/v1/videos`

#### 整文件上传

```
POST /api/v1/videos/upload
Content-Type: multipart/form-data

file        视频文件
projectNo   项目编号（必填）
patientCode 受试者编码（必填）
tpStage     访视点（必填）
```

#### 分片上传

```
POST /api/v1/videos/upload/chunk
Content-Type: multipart/form-data

file        分片文件
projectNo   项目编号（必填）
patientCode 受试者编码（必填）
tpStage     访视点（必填）
filename    原始文件名（必填）
uuid        上传会话唯一标识（必填）
chunk       当前分片索引（从 0 开始）
chunks      总分片数
visit       访视信息（可选）
```

#### 查询任务状态

```
GET /api/v1/videos/tasks/{taskId}
```

#### 根据任务 ID 查询播放 URL

```
GET /api/v1/videos/tasks/{taskId}/play-urls
```

#### 根据业务参数查询播放 URL

```
GET /api/v1/videos/play-urls?projectNo=&patientCode=&tpStage=&versionNo=&quality=
```

> `tpStage` 与 `visit` 必须且只能传一个。

---

### 项目配置 `/project`

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/project` | 创建项目配置 |
| `GET` | `/project/{id}` | 查询项目配置 |
| `GET` | `/project` | 查询所有项目配置 |
| `PUT` | `/project/{id}` | 更新项目配置 |
| `DELETE` | `/project/{id}` | 删除项目配置 |

---

## 监控与运维

### Actuator 端点

| 端点 | 说明 |
|------|------|
| `GET /actuator/health` | 应用健康状态 |
| `GET /actuator/metrics` | 系统指标 |
| `GET /actuator/prometheus` | Prometheus 格式指标 |
| `GET /api/v1/monitor/overview` | 业务监控概览 |
| `GET /api/v1/monitor/performance` | 性能指标 |

### 定时清理

应用内置孤立分片与无任务原始文件的定时清理任务（默认每 30 分钟执行一次），可通过以下配置调整：

```yaml
maintenance:
  cleanup:
    enabled: true
    cron: "0 0/30 * * * ?"
    orphan-chunk-ttl-minutes: 60
    orphan-original-ttl-hours: 24
    dry-run: true   # 生产上线前建议先以 dry-run 模式验证
```

---

## 构建与测试

### 编译

```bash
mvn clean compile
```

### 单元测试

```bash
mvn test
```

### 集成测试

集成测试依赖 Docker（通过 Testcontainers 自动启动 MySQL 容器）：

```bash
mvn verify
```

### 一键测试脚本

```bash
# 开发环境
./test-runner.sh dev

# 测试环境
./test-runner.sh test

# 生产环境
./test-runner.sh prod
```

脚本将依次完成：编译 → 单元测试 → 集成测试 → 启动应用 → 监控端点验证。

### 代码质量

```bash
# SpotBugs 静态分析
mvn spotbugs:check

# PMD 代码分析
mvn pmd:check

# 代码覆盖率报告（生成于 target/site/jacoco/）
mvn jacoco:report
```

---

## 项目结构

```
video_convert_platform/
├── src/
│   ├── main/
│   │   ├── java/com/fab/video_convert_platform/
│   │   │   ├── config/          # 配置类（NFS、线程池、MQ、监控等）
│   │   │   ├── common/          # 公共组件（统一响应、异常处理、错误码）
│   │   │   ├── domain/          # 领域模型（DDD 风格）
│   │   │   ├── infra/           # 基础设施层（MQ、FFmpeg、NFS、仓储实现）
│   │   │   ├── interfaces/      # 接口层（REST Controller、DTO）
│   │   │   ├── service/         # 应用服务层
│   │   │   ├── util/            # 工具类
│   │   │   └── maintenance/     # 运维维护（定时清理）
│   │   └── resources/
│   │       ├── application.yml         # 公共配置
│   │       ├── application-dev.yml     # 开发环境配置
│   │       ├── application-test.yml    # 测试环境配置
│   │       ├── application-prod.yml    # 生产环境配置
│   │       └── logback-spring.xml      # 日志配置
│   └── test/                    # 测试代码
├── .env.example                 # 环境变量模板
├── test-runner.sh               # 一键测试脚本
├── pom.xml
└── README.md
```

---

## License

本项目仅供内部使用。
