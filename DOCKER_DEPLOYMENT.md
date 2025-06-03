# Emby Balance Docker部署指南

本文档详细说明如何使用Docker部署Emby下载带宽统计与智能路由系统。

## 系统架构

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   客户端请求     │───▶│  emby-balance    │───▶│   emby-server   │
│                │    │  (Nginx + Java)  │    │                │
└─────────────────┘    └──────────────────┘    └─────────────────┘
                              │
                              ▼
                       ┌──────────────────┐
                       │  nginx-download  │
                       │  (文件下载+统计)  │
                       └──────────────────┘
                              │
                              ▼
                       ┌──────────────────┐
                       │     MySQL        │
                       │   (统计数据)     │
                       └──────────────────┘
```

## 容器说明

### 1. emby-balance
- **功能**: Nginx代理 + Spring Boot应用
- **端口**: 80 (Nginx), 8080 (Spring Boot)
- **职责**: 
  - 劫持PlaybackInfo请求
  - 智能路由选择
  - 统计数据分析
  - 性能监控API

### 2. nginx-download
- **功能**: 文件下载服务 + 统计收集
- **端口**: 8081 (映射到容器80端口)
- **职责**:
  - 处理文件下载请求
  - 收集下载统计数据
  - 发送统计到Spring Boot应用

### 3. mysql
- **功能**: 数据存储
- **端口**: 3306
- **职责**:
  - 存储路由配置
  - 存储下载统计数据
  - 用户管理数据

### 4. emby-server (可选)
- **功能**: Emby媒体服务器
- **端口**: 8096, 8920
- **说明**: 如果您已有Emby服务器，可以移除此容器

## 快速部署

### 1. 准备工作

```bash
# 克隆项目
git clone <your-repo-url>
cd emby_balance

# 确保Docker和Docker Compose已安装
docker --version
docker-compose --version
```

### 2. 配置媒体文件路径

编辑 `docker-compose.yml` 文件，修改媒体文件挂载路径：

```yaml
volumes:
  media_files:
    driver: local
    driver_opts:
      type: none
      o: bind
      device: /path/to/your/media/files  # 修改为您的媒体文件路径
```

### 3. 启动服务

```bash
# 构建并启动所有服务
docker-compose up -d

# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f
```

### 4. 验证部署

```bash
# 检查服务健康状态
curl http://localhost:8080/actuator/health
curl http://localhost/health
curl http://localhost:8081/health

# 访问Emby（如果部署了emby-server）
open http://localhost:8096
```

## 配置说明

### 环境变量

在 `docker-compose.yml` 中可以配置以下环境变量：

#### emby-balance服务
```yaml
environment:
  JAVA_OPTS: "-Xmx1g -Xms512m"                    # JVM参数
  SPRING_PROFILES_ACTIVE: docker                  # Spring配置文件
  SPRING_DATASOURCE_URL: jdbc:mysql://...         # 数据库连接
  SPRING_DATASOURCE_USERNAME: emby_user           # 数据库用户名
  SPRING_DATASOURCE_PASSWORD: emby_password       # 数据库密码
  EMBY_SERVER_HOST: emby-server                   # Emby服务器地址
  EMBY_SERVER_PORT: 8096                          # Emby服务器端口
```

#### nginx-download服务
```yaml
environment:
  SPRING_BOOT_HOST: emby-balance                  # Spring Boot服务地址
  SPRING_BOOT_PORT: 8080                          # Spring Boot服务端口
  MEDIA_ROOT: /var/lib/emby/media                 # 媒体文件根目录
```

### 数据库配置

默认数据库配置：
- 数据库名: `emby_balance`
- 用户名: `emby_user`
- 密码: `emby_password`
- Root密码: `emby_balance_root`

### 路由配置

系统启动后会自动创建示例路由，您可以通过API或数据库直接修改：

```sql
-- 查看当前路由
SELECT * FROM download_route;

-- 添加新路由
INSERT INTO download_route (name, protocol, domain, port, weight, enabled) 
VALUES ('新线路', 'http', 'your-cdn.com', 80, 10, TRUE);
```

## 管理操作

### 查看日志

```bash
# 查看所有服务日志
docker-compose logs -f

# 查看特定服务日志
docker-compose logs -f emby-balance
docker-compose logs -f nginx-download
docker-compose logs -f mysql
```

### 重启服务

```bash
# 重启所有服务
docker-compose restart

# 重启特定服务
docker-compose restart emby-balance
```

### 更新服务

```bash
# 停止服务
docker-compose down

# 重新构建镜像
docker-compose build --no-cache

# 启动服务
docker-compose up -d
```

### 数据备份

```bash
# 备份MySQL数据
docker exec emby-mysql mysqldump -u root -pemby_balance_root emby_balance > backup.sql

# 恢复数据
docker exec -i emby-mysql mysql -u root -pemby_balance_root emby_balance < backup.sql
```

## API接口

### 路由管理
```bash
# 获取路由列表
curl http://localhost:8080/api/routes

# 获取路由性能排名
curl http://localhost:8080/api/performance/routes/ranking
```

### 统计数据
```bash
# 获取系统性能概览
curl http://localhost:8080/api/performance/overview

# 获取客户端统计
curl http://localhost:8080/api/performance/clients/192.168.1.100
```

### 健康检查
```bash
# Spring Boot健康检查
curl http://localhost:8080/actuator/health

# Nginx健康检查
curl http://localhost/health
curl http://localhost:8081/health
```

## 监控和告警

### Prometheus指标

系统暴露了Prometheus指标端点：
```bash
curl http://localhost:8080/actuator/prometheus
```

### 日志监控

重要日志文件位置：
- Spring Boot: `/var/log/emby-balance.log`
- Nginx访问日志: `/var/log/nginx/access.log`
- Nginx下载统计: `/var/log/nginx/download_stats.log`
- Supervisor: `/var/log/supervisor/`

## 故障排除

### 常见问题

1. **服务启动失败**
   ```bash
   # 检查容器状态
   docker-compose ps
   
   # 查看错误日志
   docker-compose logs <service-name>
   ```

2. **数据库连接失败**
   ```bash
   # 检查MySQL是否启动
   docker-compose exec mysql mysql -u root -p -e "SHOW DATABASES;"
   ```

3. **文件下载404错误**
   - 检查媒体文件路径挂载是否正确
   - 确认文件权限设置
   - 查看nginx-download日志

4. **统计数据未记录**
   - 检查nginx-download到emby-balance的网络连接
   - 查看统计API是否正常工作
   - 检查Lua模块是否正确安装

### 性能优化

1. **调整JVM参数**
   ```yaml
   environment:
     JAVA_OPTS: "-Xmx2g -Xms1g -XX:+UseG1GC"
   ```

2. **数据库优化**
   ```yaml
   environment:
     MYSQL_INNODB_BUFFER_POOL_SIZE: 1G
   ```

3. **Nginx优化**
   - 调整worker_processes
   - 优化缓冲区设置
   - 启用gzip压缩

## 安全建议

1. **修改默认密码**
   - 数据库密码
   - 管理员账户密码

2. **网络安全**
   - 使用防火墙限制端口访问
   - 配置SSL/TLS证书
   - 设置访问控制

3. **数据安全**
   - 定期备份数据
   - 监控异常访问
   - 设置日志轮转

## 扩展部署

### 高可用部署

```yaml
# 多实例部署示例
services:
  emby-balance-1:
    # ... 配置
  emby-balance-2:
    # ... 配置
  
  nginx-lb:
    image: nginx:alpine
    # 负载均衡配置
```

### 集群部署

使用Docker Swarm或Kubernetes进行集群部署，详细配置请参考相应的编排工具文档。

## 技术支持

如遇到问题，请：
1. 查看相关日志文件
2. 检查网络连接
3. 验证配置文件
4. 参考故障排除章节

更多技术细节请参考项目源代码和相关文档。