-- Emby Balance数据库初始化脚本

-- 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS emby_balance CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 使用数据库
USE emby_balance;

-- 创建下载路由表
CREATE TABLE IF NOT EXISTS download_route (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL COMMENT '路由名称',
    protocol VARCHAR(10) NOT NULL DEFAULT 'http' COMMENT '协议',
    domain VARCHAR(255) NOT NULL COMMENT '域名',
    port INT NOT NULL DEFAULT 80 COMMENT '端口',
    weight INT NOT NULL DEFAULT 1 COMMENT '权重',
    enabled BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否启用',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_enabled (enabled),
    INDEX idx_weight (weight)
) ENGINE=InnoDB COMMENT='下载路由配置表';

-- 创建下载统计表
CREATE TABLE IF NOT EXISTS download_statistics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    route_id BIGINT NOT NULL COMMENT '路由ID',
    client_ip VARCHAR(45) NOT NULL COMMENT '客户端IP',
    download_size BIGINT NOT NULL DEFAULT 0 COMMENT '下载大小（字节）',
    download_duration BIGINT NOT NULL DEFAULT 0 COMMENT '下载时长（毫秒）',
    response_time BIGINT NOT NULL DEFAULT 0 COMMENT '响应时间（毫秒）',
    success BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否成功',
    file_path TEXT COMMENT '文件路径',
    user_agent TEXT COMMENT '用户代理',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_route_id (route_id),
    INDEX idx_client_ip (client_ip),
    INDEX idx_created_at (created_at),
    INDEX idx_success (success),
    INDEX idx_route_created (route_id, created_at)
) ENGINE=InnoDB COMMENT='下载统计表';

-- 插入示例下载路由数据
INSERT INTO download_route (name, protocol, domain, port, weight, enabled) VALUES
('高速线路1', 'http', 'cdn1.example.com', 80, 10, TRUE),
('高速线路2', 'http', 'cdn2.example.com', 80, 8, TRUE),
('备用线路1', 'http', 'backup1.example.com', 80, 5, TRUE),
('备用线路2', 'http', 'backup2.example.com', 80, 3, TRUE)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    protocol = VALUES(protocol),
    domain = VALUES(domain),
    port = VALUES(port),
    weight = VALUES(weight),
    enabled = VALUES(enabled);

-- 创建用户表（如果需要用户管理功能）
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码（加密）',
    email VARCHAR(100) COMMENT '邮箱',
    role VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT '角色',
    enabled BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否启用',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_username (username),
    INDEX idx_enabled (enabled)
) ENGINE=InnoDB COMMENT='用户表';

-- 插入默认管理员用户（密码: admin123）
INSERT INTO users (username, password, email, role, enabled) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa', 'admin@example.com', 'ADMIN', TRUE)
ON DUPLICATE KEY UPDATE
    password = VALUES(password),
    email = VALUES(email),
    role = VALUES(role),
    enabled = VALUES(enabled);

-- 创建系统配置表
CREATE TABLE IF NOT EXISTS system_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_key VARCHAR(100) NOT NULL UNIQUE COMMENT '配置键',
    config_value TEXT COMMENT '配置值',
    description VARCHAR(255) COMMENT '描述',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_config_key (config_key)
) ENGINE=InnoDB COMMENT='系统配置表';

-- 插入默认系统配置
INSERT INTO system_config (config_key, config_value, description) VALUES
('statistics.retention.days', '30', '统计数据保留天数'),
('route.selection.algorithm', 'performance', '路由选择算法：weight(权重) 或 performance(性能)'),
('performance.weight.bandwidth', '0.5', '带宽权重'),
('performance.weight.response_time', '0.3', '响应时间权重'),
('performance.weight.success_rate', '0.2', '成功率权重'),
('statistics.min_samples', '10', '性能评估最小样本数'),
('client.abuse.max_downloads_per_hour', '1000', '客户端每小时最大下载次数')
ON DUPLICATE KEY UPDATE
    config_value = VALUES(config_value),
    description = VALUES(description);

-- 创建索引优化查询性能
CREATE INDEX IF NOT EXISTS idx_statistics_performance ON download_statistics (route_id, success, created_at);
CREATE INDEX IF NOT EXISTS idx_statistics_client_analysis ON download_statistics (client_ip, created_at);

-- 显示创建的表
SHOW TABLES;

-- 显示表结构
DESCRIBE download_route;
DESCRIBE download_statistics;
DESCRIBE users;
DESCRIBE system_config;