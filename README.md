# Emby Balance - 智能负载均衡系统

## 项目简介

Emby Balance 是一个为 Emby 媒体服务器设计的智能负载均衡系统，通过多个下载节点分发媒体文件，提供高可用性和性能优化的媒体服务。

## 系统架构

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Emby Client   │───▶│  Nginx Proxy    │───▶│   Emby Server   │
└─────────────────┘    │  (Port 80)      │    │  (Port 8096)    │
                       └─────────────────┘    └─────────────────┘
                                │
                                ▼
                       ┌─────────────────┐
                       │ Spring Boot App │
                       │   (Port 8080)   │
                       └─────────────────┘
                                │
                                ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│ Download Node 1 │    │ Download Node 2 │    │ Download Node N │
│   (Port 8081)   │    │   (Port 8082)   │    │   (Port 808N)   │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

## 核心功能

### 🎯 智能路由选择
- **性能监控**: 实时监控各下载节点的响应时间、成功率、负载情况
- **动态权重**: 基于节点性能自动调整路由权重
- **故障转移**: 自动检测节点故障并切换到健康节点
- **地理位置优化**: 支持基于客户端地理位置的就近路由

### 📊 统计分析
- **下载统计**: 详细记录每次下载的文件、大小、耗时、成功率
- **性能分析**: 提供节点性能趋势分析和报告
- **用户行为**: 分析用户访问模式和热门内容
- **异常检测**: 自动识别异常下载行为和潜在问题

### 🔧 管理功能
- **路由管理**: 动态添加、删除、修改下载路由
- **配置管理**: 灵活的系统配置和参数调整
- **监控面板**: 实时监控系统状态和性能指标
- **日志管理**: 完整的操作日志和错误追踪

## 技术栈

### 后端服务
- **Spring Boot 3.x**: 主要应用框架
- **Spring Data JPA**: 数据持久化
- **MySQL 8.0**: 主数据库
- **Redis**: 缓存和会话管理
- **Spring Security**: 安全认证
- **Micrometer**: 监控指标

### 前端代理
- **Nginx**: 反向代理和负载均衡
- **Lua**: 动态路由和统计收集
- **OpenResty**: 高性能Web平台

### 容器化
- **Docker**: 容器化部署
- **Docker Compose**: 服务编排
- **Multi-stage Build**: 优化镜像大小

## 快速开始

### 环境要求

- Docker 20.10+
- Docker Compose 2.0+
- 至少 4GB 可用内存
- 至少 10GB 可用磁盘空间

### 一键部署

#### Linux/macOS
```bash
# 克隆项目
git clone <repository-url>
cd emby_balance

# 配置媒体文件路径
cp .env.example .env
vim .env  # 编辑配置文件

# 一键部署
./scripts/build-and-deploy.sh
```

#### Windows
```cmd
REM 克隆项目
git clone <repository-url>
cd emby_balance

REM 配置媒体文件路径
copy .env.example .env
notepad .env

REM 一键部署
scripts\build-and-deploy.bat
```

### 手动部署

1. **准备配置文件**
```bash
# 复制环境配置
cp .env.example .env

# 编辑配置
vim .env
```

2. **构建镜像**
```bash
# 构建所有镜像
docker-compose build

# 或分别构建
docker build -t emby-balance/nginx-download:latest -f docker/nginx-download/Dockerfile .
docker build -t emby-balance/emby-balance:latest -f docker/emby-balance/Dockerfile .
```

3. **启动服务**
```bash
# 启动所有服务
docker-compose up -d

# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f
```

## 配置说明

### 环境变量配置

创建 `.env` 文件并配置以下变量：

```env
# 数据库配置
MYSQL_ROOT_PASSWORD=your_secure_password
MYSQL_DATABASE=emby_balance
MYSQL_USER=emby_user
MYSQL_PASSWORD=your_user_password

# 应用配置
SPRING_PROFILES_ACTIVE=docker
JAVA_OPTS=-Xmx2g -Xms1g

# Emby服务器配置
EMBY_SERVER_HOST=your_emby_host
EMBY_SERVER_PORT=8096

# 媒体文件路径
MEDIA_ROOT_PATH=/path/to/your/media

# 下载服务配置
NGINX_DOWNLOAD_HOST=nginx-download
NGINX_DOWNLOAD_PORT=80

# 安全配置
JWT_SECRET=your_jwt_secret_key
ADMIN_PASSWORD=your_admin_password
```

### 媒体文件配置

确保媒体文件路径正确映射：

```yaml
# docker-compose.yml 中的卷映射
volumes:
  - "/your/media/path:/media:ro"  # 只读挂载
  - "/your/config/path:/config"   # 配置文件
```

## 服务访问

部署完成后，可以通过以下地址访问服务：

- **Emby代理服务**: http://localhost
- **管理API**: http://localhost:8080
- **下载服务**: http://localhost:8081
- **健康检查**: http://localhost:8080/actuator/health

## API 接口

### 路由管理

```bash
# 获取所有路由
curl http://localhost:8080/api/routes

# 添加路由
curl -X POST http://localhost:8080/api/routes \
  -H "Content-Type: application/json" \
  -d '{
    "name": "节点1",
    "baseUrl": "http://download1.example.com",
    "weight": 100,
    "enabled": true
  }'

# 更新路由
curl -X PUT http://localhost:8080/api/routes/1 \
  -H "Content-Type: application/json" \
  -d '{
    "weight": 150,
    "enabled": true
  }'

# 删除路由
curl -X DELETE http://localhost:8080/api/routes/1
```

### 性能监控

```bash
# 获取性能概览
curl http://localhost:8080/api/performance/overview

# 获取节点统计
curl http://localhost:8080/api/performance/nodes

# 获取下载统计
curl http://localhost:8080/api/statistics/summary
```

## 监控和日志

### 日志查看

```bash
# 查看所有服务日志
docker-compose logs -f

# 查看特定服务日志
docker-compose logs -f emby-balance
docker-compose logs -f nginx-download
docker-compose logs -f mysql

# 查看应用日志文件
docker exec -it emby-balance-app tail -f /var/log/emby-balance.log
```

### 性能监控

系统提供 Prometheus 指标端点：

```bash
# 获取 Prometheus 指标
curl http://localhost:8080/actuator/prometheus
```

可以集成 Grafana 进行可视化监控。

## 故障排除

### 常见问题

1. **服务启动失败**
```bash
# 检查服务状态
docker-compose ps

# 查看错误日志
docker-compose logs service-name

# 重启服务
docker-compose restart service-name
```

2. **数据库连接失败**
```bash
# 检查MySQL服务
docker-compose exec mysql mysqladmin ping

# 检查数据库配置
docker-compose exec mysql mysql -u root -p -e "SHOW DATABASES;"
```

3. **路由不工作**
```bash
# 检查路由配置
curl http://localhost:8080/api/routes

# 测试下载节点
curl -I http://download-node-url/health
```

### 性能优化

1. **调整JVM参数**
```env
# .env 文件中
JAVA_OPTS=-Xmx4g -Xms2g -XX:+UseG1GC
```

2. **调整数据库配置**
```sql
-- 优化MySQL配置
SET GLOBAL innodb_buffer_pool_size = 1073741824;  -- 1GB
SET GLOBAL max_connections = 200;
```

3. **调整Nginx配置**
```nginx
# 增加worker进程数
worker_processes auto;
worker_connections 1024;
```

## 开发指南

### 本地开发环境

1. **环境要求**
   - JDK 17+
   - Maven 3.8+
   - Node.js 16+ (如果需要前端开发)
   - MySQL 8.0+
   - Redis 6.0+

2. **启动开发环境**
```bash
# 启动数据库服务
docker-compose up -d mysql redis

# 运行Spring Boot应用
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# 或使用IDE运行主类
# com.emby.balance.EmbyBalanceApplication
```

3. **代码结构**
```
src/main/java/com/emby/balance/
├── config/          # 配置类
├── controller/      # REST控制器
├── service/         # 业务逻辑
├── repository/      # 数据访问
├── entity/          # 实体类
├── dto/             # 数据传输对象
└── util/            # 工具类
```

### 贡献指南

1. Fork 项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 打开 Pull Request

## 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。

## 支持

如果您遇到问题或有建议，请：

1. 查看 [FAQ](docs/FAQ.md)
2. 搜索 [Issues](../../issues)
3. 创建新的 [Issue](../../issues/new)
4. 加入讨论 [Discussions](../../discussions)

## 更新日志

查看 [CHANGELOG.md](CHANGELOG.md) 了解版本更新信息。

---

**注意**: 请确保在生产环境中更改默认密码和密钥，并定期备份数据。