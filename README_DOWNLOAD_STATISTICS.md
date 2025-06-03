# Emby下载带宽统计与智能路由系统

本系统实现了基于下载带宽统计的智能路由选择功能，能够根据各个IP地址的下载情况，优先返回下载速度快的下载路线。

## 功能特性

### 1. 下载统计收集
- 记录每次下载的带宽、响应时间、成功率等指标
- 支持按IP地址、路由ID进行统计分析
- 自动清理过期统计数据

### 2. 智能路由选择
- 基于历史统计数据计算路由性能评分
- 综合考虑带宽、响应时间、成功率等因素
- 使用加权随机算法选择最优路由
- 当统计数据不足时自动回退到权重选择

### 3. 性能监控
- 提供路由性能排名API
- 支持查看单个路由的详细报告
- 客户端下载统计分析
- 系统整体性能概览

## 系统架构

```
客户端请求 → Nginx → Spring Boot应用 → 选择最优路由 → 返回下载URL
     ↓
下载文件 → Nginx记录统计 → 发送到Spring Boot → 更新统计数据
```

## 核心组件

### 1. 数据模型

#### DownloadStatistics 实体
- `routeId`: 路由ID
- `clientIp`: 客户端IP地址
- `downloadSize`: 下载大小（字节）
- `downloadDuration`: 下载时长（毫秒）
- `responseTime`: 响应时间（毫秒）
- `success`: 是否成功
- `filePath`: 文件路径
- `userAgent`: 用户代理
- `createdAt`: 创建时间

### 2. 服务层

#### DownloadStatisticsService
- `recordDownloadStatistics()`: 记录下载统计
- `getAverageBandwidth()`: 计算平均带宽
- `getAverageResponseTime()`: 计算平均响应时间
- `getSuccessRate()`: 计算成功率
- `getRoutePerformanceScore()`: 计算路由性能评分
- `cleanupOldStatistics()`: 清理过期数据

#### DownloadRouteService (增强)
- `selectRouteByPerformance()`: 基于性能选择路由
- `getRoutePerformanceReport()`: 获取路由性能报告
- `getRoutePerformanceRanking()`: 获取路由性能排名

### 3. 控制器

#### StatisticsController
- `POST /api/statistics/record`: 记录单条统计数据
- `POST /api/statistics/record/batch`: 批量记录统计数据

#### PerformanceController
- `GET /api/performance/routes/ranking`: 路由性能排名
- `GET /api/performance/routes/{routeId}/report`: 单个路由报告
- `GET /api/performance/overview`: 系统性能概览
- `DELETE /api/performance/cleanup`: 清理过期数据
- `GET /api/performance/clients/{clientIp}`: 客户端统计

## 部署配置

### 1. Spring Boot应用配置

确保以下组件已正确配置：

```java
// 启用定时任务
@EnableScheduling

// 数据库配置
spring.datasource.url=jdbc:mysql://localhost:3306/emby_balance
spring.jpa.hibernate.ddl-auto=update
```

### 2. Nginx配置

#### 方案A：使用Lua模块（推荐）

使用 `nginx-statistics.conf.example` 配置文件：

```bash
# 安装lua-resty-http模块
luarocks install lua-resty-http

# 确保nginx编译时包含lua支持
nginx -V 2>&1 | grep -o with-http_lua_module
```

#### 方案B：使用日志分析

1. 使用 `nginx-download.conf.example` 基础配置
2. 设置定时任务解析日志：

```bash
# 添加到crontab
*/5 * * * * /usr/bin/python3 /path/to/scripts/parse_nginx_logs.py /var/log/nginx/download_stats.log
```

### 3. 数据库表结构

系统会自动创建以下表：

```sql
CREATE TABLE download_statistics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    route_id BIGINT NOT NULL,
    client_ip VARCHAR(45) NOT NULL,
    download_size BIGINT NOT NULL,
    download_duration BIGINT NOT NULL,
    response_time BIGINT NOT NULL,
    success BOOLEAN NOT NULL,
    file_path TEXT,
    user_agent TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_route_id (route_id),
    INDEX idx_client_ip (client_ip),
    INDEX idx_created_at (created_at)
);
```

## 使用示例

### 1. 查看路由性能排名

```bash
curl http://localhost:8080/api/performance/routes/ranking
```

响应示例：
```json
[
  {
    "routeId": 1,
    "routeName": "高速线路1",
    "performanceScore": 85.5,
    "averageBandwidth": 1024000,
    "averageResponseTime": 150,
    "successRate": 0.98,
    "totalDownloads": 1500
  }
]
```

### 2. 查看单个路由详细报告

```bash
curl http://localhost:8080/api/performance/routes/1/report?days=7
```

### 3. 查看系统性能概览

```bash
curl http://localhost:8080/api/performance/overview
```

### 4. 查看客户端统计

```bash
curl http://localhost:8080/api/performance/clients/192.168.1.100?days=30
```

## 性能评分算法

系统使用综合评分算法选择最优路由：

```
性能评分 = (带宽权重 × 带宽评分) + (响应时间权重 × 响应时间评分) + (成功率权重 × 成功率评分)

其中：
- 带宽权重: 0.5
- 响应时间权重: 0.3  
- 成功率权重: 0.2

各项评分范围: 0-100
```

## 监控和维护

### 1. 定时清理

系统每天凌晨2点自动清理30天前的统计数据：

```java
@Scheduled(cron = "0 0 2 * * ?")
public void cleanupOldStatistics()
```

### 2. 手动清理

```bash
curl -X DELETE http://localhost:8080/api/performance/cleanup
```

### 3. 日志监控

监控以下日志文件：
- Spring Boot应用日志
- Nginx访问日志 `/var/log/nginx/download_stats.log`
- 统计脚本日志

## 故障排除

### 1. 统计数据未记录

检查项：
- Nginx配置是否正确
- Spring Boot应用是否运行
- 网络连接是否正常
- 日志文件权限是否正确

### 2. 路由选择不准确

检查项：
- 统计数据是否充足（建议至少100次下载）
- 时间范围设置是否合理
- 路由权重配置是否正确

### 3. 性能问题

优化建议：
- 定期清理过期数据
- 为数据库表添加适当索引
- 调整统计数据保留时间
- 使用数据库连接池

## 扩展功能

### 1. 地理位置优化

可以根据客户端地理位置选择最近的路由：

```java
// 在DownloadStatistics中添加地理位置字段
private String country;
private String region;
private String city;
```

### 2. 时间段分析

分析不同时间段的网络状况：

```java
// 添加时间段统计
public Map<String, Double> getHourlyPerformance(Long routeId, int days)
```

### 3. 实时监控

使用WebSocket推送实时性能数据：

```java
@Controller
public class PerformanceWebSocketController {
    // 实时推送性能数据
}
```

## 注意事项

1. **隐私保护**: IP地址等敏感信息需要符合相关法律法规
2. **数据安全**: 统计数据应定期备份
3. **性能影响**: 统计收集不应影响正常下载性能
4. **容错处理**: 统计系统故障不应影响主要功能
5. **资源管理**: 合理控制统计数据的存储空间

## 版本历史

- v1.0: 基础统计功能
- v1.1: 智能路由选择
- v1.2: 性能监控API
- v1.3: 自动清理和维护

## 技术支持

如有问题，请检查：
1. 应用日志文件
2. 数据库连接状态
3. Nginx配置正确性
4. 网络连通性

更多技术细节请参考源代码注释和相关文档。