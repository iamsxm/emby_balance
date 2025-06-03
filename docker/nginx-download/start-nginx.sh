#!/bin/sh

# Nginx下载模块启动脚本

echo "正在启动Nginx下载和统计模块..."

# 检查环境变量
echo "环境变量配置:"
echo "EMBY_SERVER_HOST: ${EMBY_SERVER_HOST:-emby-server}"
echo "EMBY_SERVER_PORT: ${EMBY_SERVER_PORT:-8096}"
echo "SPRING_BOOT_HOST: ${SPRING_BOOT_HOST:-emby-balance}"
echo "SPRING_BOOT_PORT: ${SPRING_BOOT_PORT:-8080}"
echo "MEDIA_ROOT: ${MEDIA_ROOT:-/var/lib/emby/media}"

# 创建必要的目录
mkdir -p /var/log/nginx
mkdir -p "${MEDIA_ROOT:-/var/lib/emby/media}"
mkdir -p /tmp

# 设置权限
chown -R nginx:nginx /var/log/nginx
chown -R nginx:nginx "${MEDIA_ROOT:-/var/lib/emby/media}"

# 测试Nginx配置
echo "测试Nginx配置..."
nginx -t
if [ $? -ne 0 ]; then
    echo "Nginx配置测试失败，退出"
    exit 1
fi

# 启动crond服务（用于定时执行日志解析脚本）
echo "启动crond服务..."
crond -b

# 等待Spring Boot服务启动
echo "等待Spring Boot服务启动..."
SPRING_BOOT_URL="http://${SPRING_BOOT_HOST:-emby-balance}:${SPRING_BOOT_PORT:-8080}/actuator/health"
for i in $(seq 1 30); do
    if curl -f "$SPRING_BOOT_URL" >/dev/null 2>&1; then
        echo "Spring Boot服务已启动"
        break
    fi
    echo "等待Spring Boot服务启动... ($i/30)"
    sleep 2
done

# 启动Nginx
echo "启动Nginx..."
exec nginx -g "daemon off;"