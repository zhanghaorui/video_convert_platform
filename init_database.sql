-- ========================================
-- 视频转换平台数据库初始化脚本
-- 作者: zhanghaorui
-- 创建时间: 2024-12-19
-- 描述: 医疗视频归档处理平台数据表结构
-- ========================================

-- 删除已存在的数据库（谨慎使用）
-- DROP DATABASE IF EXISTS video_platform;

-- 创建数据库
CREATE DATABASE IF NOT EXISTS video_platform DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE video_platform;

-- ========================================
-- 1. 项目配置表 (project_config)
-- ========================================
DROP TABLE IF EXISTS project_config;
CREATE TABLE project_config (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    project_no VARCHAR(64) NOT NULL COMMENT '项目编号，唯一',
    project_name VARCHAR(128) NOT NULL COMMENT '项目名称',
    archive_root VARCHAR(256) NOT NULL COMMENT 'NFS归档根目录',
    callback_url VARCHAR(512) DEFAULT NULL COMMENT '业务系统回调地址',
    is_active TINYINT(1) DEFAULT 1 COMMENT '是否启用 1:启用 0:禁用',
    ext_json TEXT DEFAULT NULL COMMENT '扩展参数（JSON格式）',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_project_no (project_no),
    KEY idx_is_active (is_active),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='项目配置表';

-- ========================================
-- 2. 视频上传任务主表 (video_upload_task)
-- ========================================
DROP TABLE IF EXISTS video_upload_task;
CREATE TABLE video_upload_task (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    project_no VARCHAR(64) NOT NULL COMMENT '项目编号',
    patient_code VARCHAR(64) NOT NULL COMMENT '受试者编码',
    tp_stage VARCHAR(64) NOT NULL COMMENT '访视点/时间点',
    uuid VARCHAR(64) NOT NULL COMMENT '唯一标识符',
    version_no INT NOT NULL DEFAULT 1 COMMENT '版本号（初始1，业务多批次递增）',
    source VARCHAR(32) NOT NULL COMMENT '来源（CONTROLLER/MQ/其他）',
    status VARCHAR(32) NOT NULL DEFAULT 'ORIGINAL_SAVED' COMMENT '任务主状态（ORIGINAL_SAVED/PROCESSING/FINISHED/FAILED）',
    main_file_name VARCHAR(256) NOT NULL COMMENT '原始文件名',
    main_file_path VARCHAR(512) NOT NULL COMMENT '原始文件NFS路径',
    file_size BIGINT NOT NULL COMMENT '原始文件大小（字节）',
    file_md5 VARCHAR(64) NOT NULL COMMENT '原始文件MD5值',
    error_msg VARCHAR(512) DEFAULT NULL COMMENT '错误/失败信息',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_project_patient_tp_uuid (project_no, patient_code, tp_stage, uuid),
    KEY idx_project_no (project_no),
    KEY idx_patient_code (patient_code),
    KEY idx_status (status),
    KEY idx_create_time (create_time),
    KEY idx_source (source),
    CONSTRAINT fk_upload_task_project FOREIGN KEY (project_no) REFERENCES project_config (project_no) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='视频上传任务主表';

-- ========================================
-- 3. 归档文件子表 (video_archive_file)
-- ========================================
DROP TABLE IF EXISTS video_archive_file;
CREATE TABLE video_archive_file (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    task_id BIGINT NOT NULL COMMENT '所属上传任务ID',
    file_type VARCHAR(32) NOT NULL COMMENT '文件类型（ORIGINAL/M3U8/THUMBNAIL等）',
    quality_level VARCHAR(32) DEFAULT NULL COMMENT '质量级别（low/normal/high/720p/1080p等）',
    file_name VARCHAR(256) NOT NULL COMMENT '文件名',
    file_path VARCHAR(512) NOT NULL COMMENT 'NFS存储路径',
    play_url VARCHAR(1024) DEFAULT NULL COMMENT '播放URL（如适用）',
    file_size BIGINT NOT NULL COMMENT '文件大小（字节）',
    file_md5 VARCHAR(64) NOT NULL COMMENT '文件MD5值',
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态（ACTIVE/ARCHIVED/DELETED等）',
    remark VARCHAR(256) DEFAULT NULL COMMENT '备注信息',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_task_id (task_id),
    KEY idx_file_type (file_type),
    KEY idx_quality_level (quality_level),
    KEY idx_status (status),
    KEY idx_create_time (create_time),
    CONSTRAINT fk_archive_file_task FOREIGN KEY (task_id) REFERENCES video_upload_task (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='归档文件子表';

-- ========================================
-- 4. 任务日志表 (video_task_info)
-- ========================================
DROP TABLE IF EXISTS video_task_info;
CREATE TABLE video_task_info (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    task_id BIGINT NOT NULL COMMENT '关联上传任务ID',
    message TEXT NOT NULL COMMENT '日志消息内容',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '日志生成时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_task_id (task_id),
    KEY idx_create_time (create_time),
    CONSTRAINT fk_task_info_task FOREIGN KEY (task_id) REFERENCES video_upload_task (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务日志表';

-- ========================================
-- 5. 任务错误/异常表 (video_task_error)
-- ========================================
DROP TABLE IF EXISTS video_task_error;
CREATE TABLE video_task_error (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    task_id BIGINT NOT NULL COMMENT '关联上传任务ID',
    error_msg TEXT NOT NULL COMMENT '错误详细信息',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '错误发生时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_task_id (task_id),
    KEY idx_create_time (create_time),
    CONSTRAINT fk_task_error_task FOREIGN KEY (task_id) REFERENCES video_upload_task (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务错误/异常表';

-- ========================================
-- 插入初始化数据
-- ========================================

-- 插入测试项目配置
INSERT INTO project_config (project_no, project_name, archive_root, callback_url, is_active, ext_json) VALUES
('TEST_PROJECT', '测试项目', '/data/nfs-storage/test', 'http://localhost:8080/api/callback', 1, '{"description": "测试项目配置"}'),
('DEMO_PROJECT', '演示项目', '/data/nfs-storage/demo', 'http://demo.example.com/callback', 1, '{"description": "演示项目配置"}'),
('PROD_PROJECT_001', '生产项目001', '/data/nfs-storage/prod001', 'https://prod.example.com/api/v1/callback', 1, '{"description": "生产环境项目", "retention_days": 365}');

-- ========================================
-- 创建索引优化查询性能
-- ========================================

-- 任务查询优化索引
CREATE INDEX idx_task_project_status ON video_upload_task (project_no, status);
CREATE INDEX idx_task_patient_tp ON video_upload_task (patient_code, tp_stage);
CREATE INDEX idx_task_update_time ON video_upload_task (update_time);

-- 归档文件查询优化索引
CREATE INDEX idx_archive_task_type ON video_archive_file (task_id, file_type);
CREATE INDEX idx_archive_quality_status ON video_archive_file (quality_level, status);

-- 日志查询优化索引
CREATE INDEX idx_task_info_time ON video_task_info (task_id, create_time);
CREATE INDEX idx_task_error_time ON video_task_error (task_id, create_time);

-- ========================================
-- 数据库权限和用户设置（可选）
-- ========================================

-- 创建应用专用数据库用户（请根据实际情况修改密码）
-- CREATE USER 'video_app'@'%' IDENTIFIED BY 'your_secure_password_here';
-- GRANT SELECT, INSERT, UPDATE, DELETE ON video_platform.* TO 'video_app'@'%';
-- FLUSH PRIVILEGES;

-- ========================================
-- 验证表结构
-- ========================================

-- 显示所有表
SHOW TABLES;

-- 显示表结构（可选执行）
-- DESCRIBE project_config;
-- DESCRIBE video_upload_task;
-- DESCRIBE video_archive_file;
-- DESCRIBE video_task_info;
-- DESCRIBE video_task_error;

-- ========================================
-- 脚本执行完成
-- ========================================
SELECT 'Database initialization completed successfully!' AS result;
