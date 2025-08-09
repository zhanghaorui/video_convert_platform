# RabbitMQ用户权限配置指南

## 推荐的用户角色设计

### 1. 生产环境用户配置

#### 应用用户 (video_app)
```bash
# 创建应用专用用户
rabbitmqctl add_user video_app 'SecureVideoApp2025!'

# 设置用户标签 (移除administrator标签)
rabbitmqctl set_user_tags video_app ''

# 配置虚拟主机权限 (只对指定vhost有权限)
rabbitmqctl set_permissions -p /video_platform video_app "video\..*" "video\..*" "video\..*"
```

#### 监控用户 (video_monitor) 
```bash
# 创建监控用户
rabbitmqctl add_user video_monitor 'MonitorPass2025!'

# 设置监控标签
rabbitmqctl set_user_tags video_monitor monitoring

# 只读权限
rabbitmqctl set_permissions -p /video_platform video_monitor "" "" ".*"
```

#### 管理用户 (video_admin)
```bash
# 创建管理用户 (仅运维使用)
rabbitmqctl add_user video_admin 'AdminSecure2025!'

# 设置管理员标签 (仅在需要时使用)
rabbitmqctl set_user_tags video_admin administrator

# 完整权限 (仅管理员有)
rabbitmqctl set_permissions -p /video_platform video_admin ".*" ".*" ".*"
```

### 2. 权限说明

#### 权限模式: configure / write / read
- **configure**: 创建和删除队列、交换器
- **write**: 发布消息到交换器  
- **read**: 从队列消费消息

#### 应用用户权限解释
```bash
# video_app用户权限: "video\..*" "video\..*" "video\..*"
# configure: 只能配置以"video."开头的资源
# write: 只能向以"video."开头的资源写入
# read: 只能从以"video."开头的资源读取
```

### 3. 虚拟主机隔离

```bash
# 创建专用虚拟主机
rabbitmqctl add_vhost /video_platform

# 删除guest用户 (生产环境安全要求)
rabbitmqctl delete_user guest
```

## Docker环境配置

### docker-compose.yml 中的安全配置
```yaml
rabbitmq:
  image: rabbitmq:3.12-management
  environment:
    # 禁用默认用户
    RABBITMQ_DEFAULT_USER: ''
    RABBITMQ_DEFAULT_PASS: ''
    # 使用配置文件管理用户
    RABBITMQ_CONFIG_FILE: /etc/rabbitmq/rabbitmq
  volumes:
    - ./docker/rabbitmq/rabbitmq.conf:/etc/rabbitmq/rabbitmq.conf
    - ./docker/rabbitmq/definitions.json:/etc/rabbitmq/definitions.json
```

### 定义文件示例 (definitions.json)
```json
{
  "users": [
    {
      "name": "video_app",
      "password_hash": "hashed_password_here",
      "hashing_algorithm": "rabbit_password_hashing_sha256",
      "tags": ""
    },
    {
      "name": "video_monitor", 
      "password_hash": "hashed_password_here",
      "hashing_algorithm": "rabbit_password_hashing_sha256",
      "tags": "monitoring"
    }
  ],
  "vhosts": [
    {"name": "/video_platform"}
  ],
  "permissions": [
    {
      "user": "video_app",
      "vhost": "/video_platform", 
      "configure": "video\\..*",
      "write": "video\\..*",
      "read": "video\\..*"
    }
  ],
  "queues": [
    {
      "name": "video.task.queue",
      "vhost": "/video_platform",
      "durable": true,
      "auto_delete": false
    }
  ]
}
```

## 环境变量管理

### 生产环境变量 (.env.prod)
```bash
# RabbitMQ应用用户配置
RABBITMQ_APP_USER=video_app
RABBITMQ_APP_PASSWORD=SecureVideoApp2025!
RABBITMQ_VHOST=/video_platform

# RabbitMQ监控用户配置  
RABBITMQ_MONITOR_USER=video_monitor
RABBITMQ_MONITOR_PASSWORD=MonitorPass2025!

# 管理员用户 (仅运维使用)
RABBITMQ_ADMIN_USER=video_admin
RABBITMQ_ADMIN_PASSWORD=AdminSecure2025!
```

### 开发环境变量 (.env.dev)
```bash
# 开发环境可以使用简单配置
RABBITMQ_APP_USER=video_dev
RABBITMQ_APP_PASSWORD=dev123
RABBITMQ_VHOST=/video_dev
```

## 密码安全策略

### 1. 密码强度要求
- 最少12位字符
- 包含大小写字母、数字、特殊字符
- 定期轮换 (建议3-6个月)

### 2. 密码存储
- 使用环境变量，不写在代码中
- 生产环境使用密钥管理服务
- 开发环境使用.env文件 (加入.gitignore)

### 3. 连接字符串示例
```yaml
# 生产环境配置
spring:
  rabbitmq:
    host: ${RABBITMQ_HOST:prod-rabbitmq}
    port: ${RABBITMQ_PORT:5672}
    username: ${RABBITMQ_APP_USER:video_app}
    password: ${RABBITMQ_APP_PASSWORD}
    virtual-host: ${RABBITMQ_VHOST:/video_platform}
```

## 监控和审计

### 1. 启用审计日志
```ini
# rabbitmq.conf
log.file.level = info
log.connection.level = info
log.channel.level = info
```

### 2. 定期检查用户权限
```bash
# 检查用户列表
rabbitmqctl list_users

# 检查用户权限
rabbitmqctl list_permissions -p /video_platform

# 检查用户连接
rabbitmqctl list_connections user
```
