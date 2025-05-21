# Emby 请求拦截与路径修改服务

这个Spring Boot应用程序用于拦截Emby的`/Items/{itemId}/PlaybackInfo`请求，使用Emby的API密钥请求原始接口，并修改返回数据中的Path字段后返回给客户端。

## 功能特点

- 拦截Emby的PlaybackInfo请求
- 使用配置的API密钥转发请求到Emby服务器
- 修改返回数据中的媒体文件路径
- 通过Nginx反向代理实现请求劫持

## 项目结构

```
emby_balance/
├── src/main/java/com/watermelon/embybalance/
│   ├── controller/         # 控制器层
│   ├── service/            # 服务层
│   ├── config/             # 配置类
│   └── EmbyBalanceApplication.java  # 应用程序入口
├── src/main/resources/
│   └── application.properties  # 应用配置文件
├── nginx.conf.example      # Nginx配置示例
├── pom.xml                 # Maven依赖配置
└── README.md               # 项目说明文档
```

## 配置说明

### 应用配置

在`application.properties`文件中配置以下参数：

```properties
# 服务器配置
server.port=8080

# Emby服务器配置
emby.server.url=http://your-emby-server:8096  # 替换为您的Emby服务器地址
emby.api.key=your-emby-api-key  # 替换为您的Emby API密钥

# Path修改前缀（可选）
path.modification.prefix=  # 如需修改路径前缀，在此设置
```

### Nginx配置

项目提供了`nginx.conf.example`文件作为Nginx配置示例。将其中的配置添加到您的Nginx配置文件中，并根据实际情况修改：

1. 将`your-emby-server:8096`替换为您的Emby服务器地址
2. 将`your-domain.com`替换为您的域名
3. 确保`upstream emby_balance`指向Spring Boot应用的地址和端口

## 使用方法

### 1. 编译和运行

```bash
# 编译项目
mvn clean package

# 运行应用
java -jar target/emby-balance-0.0.1-SNAPSHOT.jar
```

### 2. 配置Nginx

将`nginx.conf.example`中的配置添加到您的Nginx配置文件中，并重启Nginx：

```bash
nginx -s reload
```

### 3. 测试

配置完成后，当Emby客户端发送PlaybackInfo请求时，请求将被Nginx转发到Spring Boot应用，应用会处理请求并修改返回的Path字段。

## 注意事项

- 确保Spring Boot应用和Emby服务器之间的网络连接正常
- 确保提供了正确的Emby API密钥
- 如需修改路径前缀，请在`application.properties`中设置`path.modification.prefix`属性