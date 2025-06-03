# Emby Balance Java服务 + Nginx劫持 Docker镜像
FROM openjdk:17-jdk-alpine AS builder

# 安装Maven
RUN apk add --no-cache maven

# 设置工作目录
WORKDIR /app

# 复制项目文件
COPY pom.xml .
COPY src ./src

# 构建应用
RUN mvn clean package -DskipTests

# 运行时镜像
FROM nginx:1.24-alpine

# 安装OpenJDK 17和其他必要工具
RUN apk add --no-cache \
    openjdk17-jre \
    curl \
    bash \
    supervisor

# 创建应用目录
RUN mkdir -p /app /var/log/supervisor /etc/supervisor/conf.d

# 从构建阶段复制JAR文件
COPY --from=builder /app/target/*.jar /app/emby-balance.jar

# 复制Nginx配置文件
COPY nginx-proxy.conf /etc/nginx/conf.d/default.conf
COPY nginx.conf /etc/nginx/nginx.conf

# 复制Supervisor配置
COPY supervisord.conf /etc/supervisor/conf.d/supervisord.conf

# 复制启动脚本
COPY start-services.sh /usr/local/bin/
RUN chmod +x /usr/local/bin/start-services.sh

# 创建数据目录
RUN mkdir -p /app/data

# 暴露端口
EXPOSE 80 8080

# 设置环境变量
ENV JAVA_OPTS="-Xmx512m -Xms256m" \
    SPRING_PROFILES_ACTIVE=docker \
    EMBY_SERVER_HOST=emby-server \
    EMBY_SERVER_PORT=8096 \
    NGINX_DOWNLOAD_HOST=nginx-download \
    NGINX_DOWNLOAD_PORT=80

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health && curl -f http://localhost/health || exit 1

# 启动命令
CMD ["/usr/local/bin/start-services.sh"]