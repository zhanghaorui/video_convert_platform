# 视频处理平台部署配置指南

## 数据库部署方案

### 生产环境推荐配置

#### 1. 独立MySQL服务器部署

```bash
# MySQL 8.0 安装配置
# CentOS/RHEL
sudo yum install mysql-server
sudo systemctl start mysqld
sudo systemctl enable mysqld

# Ubuntu/Debian
sudo apt update
sudo apt install mysql-server
sudo systemctl start mysql
sudo systemctl enable mysql
```

#### 2. MySQL配置优化 (/etc/mysql/mysql.conf.d/mysqld.cnf)

```ini
[mysqld]
# 基础配置
server-id = 1
port = 3306
bind-address = 0.0.0.0

# 字符集配置
character-set-server = utf8mb4
collation-server = utf8mb4_unicode_ci

# 内存配置 (根据服务器内存调整)
innodb_buffer_pool_size = 2G
innodb_log_file_size = 256M
innodb_log_buffer_size = 16M

# 连接配置
max_connections = 500
max_connect_errors = 100000

# 查询优化
query_cache_type = 1
query_cache_size = 128M
tmp_table_size = 128M
max_heap_table_size = 128M

# 日志配置
slow_query_log = 1
slow_query_log_file = /var/log/mysql/mysql-slow.log
long_query_time = 2

# 二进制日志 (用于备份和主从复制)
log-bin = mysql-bin
binlog_format = ROW
expire_logs_days = 7

# 安全配置
sql_mode = STRICT_TRANS_TABLES,NO_ZERO_DATE,NO_ZERO_IN_DATE,ERROR_FOR_DIVISION_BY_ZERO
```

#### 3. 数据库安全配置

```sql
-- 创建应用专用用户
CREATE USER 'video_app'@'%' IDENTIFIED BY 'your_secure_password_here';
GRANT SELECT, INSERT, UPDATE, DELETE ON video_platform.* TO 'video_app'@'%';

-- 创建只读用户（用于监控和报表）
CREATE USER 'video_readonly'@'%' IDENTIFIED BY 'readonly_password';
GRANT SELECT ON video_platform.* TO 'video_readonly'@'%';

FLUSH PRIVILEGES;
```

### 开发环境配置

#### Docker Compose 部署

```yaml
version: '3.8'

services:
  # MySQL数据库
  mysql:
    image: mysql:8.0.35
    container_name: video_mysql
    restart: unless-stopped
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD:-root123}
      MYSQL_DATABASE: video_platform
      MYSQL_USER: video_app
      MYSQL_PASSWORD: ${MYSQL_APP_PASSWORD:-app123}
    volumes:
      - mysql_data:/var/lib/mysql
      - ./init_database.sql:/docker-entrypoint-initdb.d/01_init.sql
      - ./docker/mysql/conf.d:/etc/mysql/conf.d
    ports:
      - "3306:3306"
    networks:
      - video_platform
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-u", "root", "-p${MYSQL_ROOT_PASSWORD:-root123}"]
      interval: 30s
      timeout: 10s
      retries: 5

  # Redis (用于缓存和会话存储)
  redis:
    image: redis:7.2-alpine
    container_name: video_redis
    restart: unless-stopped
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
    networks:
      - video_platform
    command: redis-server --appendonly yes

  # RabbitMQ (消息队列)
  rabbitmq:
    image: rabbitmq:3.12-management
    container_name: video_rabbitmq
    restart: unless-stopped
    environment:
      RABBITMQ_DEFAULT_USER: ${RABBITMQ_USER:-admin}
      RABBITMQ_DEFAULT_PASS: ${RABBITMQ_PASSWORD:-admin123}
    ports:
      - "5672:5672"    # AMQP端口
      - "15672:15672"  # 管理界面
    volumes:
      - rabbitmq_data:/var/lib/rabbitmq
    networks:
      - video_platform

  # 应用服务
  app:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: video_app
    restart: unless-stopped
    depends_on:
      mysql:
        condition: service_healthy
      redis:
        condition: service_started
      rabbitmq:
        condition: service_started
    environment:
      SPRING_PROFILES_ACTIVE: ${SPRING_PROFILE:-dev}
      DB_HOST: mysql
      DB_PORT: 3306
      DB_NAME: video_platform
      DB_USERNAME: video_app
      DB_PASSWORD: ${MYSQL_APP_PASSWORD:-app123}
      REDIS_HOST: redis
      REDIS_PORT: 6379
      RABBITMQ_HOST: rabbitmq
      RABBITMQ_PORT: 5672
      RABBITMQ_USERNAME: ${RABBITMQ_USER:-admin}
      RABBITMQ_PASSWORD: ${RABBITMQ_PASSWORD:-admin123}
    ports:
      - "8080:8080"
    volumes:
      - app_logs:/app/logs
      - nfs_storage:/data/nfs-storage
    networks:
      - video_platform

volumes:
  mysql_data:
    driver: local
  redis_data:
    driver: local
  rabbitmq_data:
    driver: local
  app_logs:
    driver: local
  nfs_storage:
    driver: local

networks:
  video_platform:
    driver: bridge
```

### 高可用部署配置

#### 主从复制配置

```ini
# 主库配置 (master)
[mysqld]
server-id = 1
log-bin = mysql-bin
binlog_format = ROW
gtid_mode = ON
enforce_gtid_consistency = ON

# 从库配置 (slave)
[mysqld]
server-id = 2
relay-log = mysql-relay-bin
log-slave-updates = 1
read_only = 1
gtid_mode = ON
enforce_gtid_consistency = ON
```

### 备份策略

#### 1. 自动备份脚本

```bash
#!/bin/bash
# backup_db.sh

DB_NAME="video_platform"
DB_USER="root"
DB_PASSWORD="your_password"
BACKUP_DIR="/backup/mysql"
DATE=$(date +%Y%m%d_%H%M%S)

# 创建备份目录
mkdir -p ${BACKUP_DIR}

# 全量备份
mysqldump -u${DB_USER} -p${DB_PASSWORD} \
  --single-transaction \
  --routines \
  --triggers \
  --events \
  --hex-blob \
  ${DB_NAME} > ${BACKUP_DIR}/${DB_NAME}_${DATE}.sql

# 压缩备份文件
gzip ${BACKUP_DIR}/${DB_NAME}_${DATE}.sql

# 删除7天前的备份
find ${BACKUP_DIR} -name "*.sql.gz" -mtime +7 -delete

echo "Database backup completed: ${DB_NAME}_${DATE}.sql.gz"
```

#### 2. 定时任务配置

```bash
# 添加到crontab
# 每天凌晨2点执行备份
0 2 * * * /path/to/backup_db.sh >> /var/log/mysql_backup.log 2>&1
```

### 监控配置

#### Prometheus + Grafana 监控

```yaml
# prometheus.yml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: 'mysql'
    static_configs:
      - targets: ['mysql-exporter:9104']
  
  - job_name: 'video-platform'
    static_configs:
      - targets: ['app:8080']
    metrics_path: '/actuator/prometheus'
```

## 部署建议总结

### 小规模部署 (< 1000用户)
- **推荐**: Docker Compose
- **数据库**: 单机MySQL
- **存储**: 本地存储或小型NAS

### 中等规模部署 (1000-10000用户)
- **推荐**: 独立数据库服务器
- **数据库**: MySQL主从复制
- **存储**: 分布式文件系统

### 大规模部署 (> 10000用户)
- **推荐**: 云服务或K8s集群
- **数据库**: MySQL集群或云RDS
- **存储**: 对象存储服务
