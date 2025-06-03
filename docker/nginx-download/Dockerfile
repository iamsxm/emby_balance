# Nginx下载和统计模块 Docker镜像
FROM nginx:1.24-alpine

# 安装必要的依赖
RUN apk add --no-cache \
    lua5.1 \
    lua5.1-dev \
    luarocks5.1 \
    gcc \
    musl-dev \
    openssl-dev \
    pcre-dev \
    zlib-dev \
    curl \
    python3 \
    py3-pip \
    py3-requests

# 安装lua-resty-http模块
RUN luarocks-5.1 install lua-resty-http
RUN luarocks-5.1 install lua-cjson

# 创建必要的目录
RUN mkdir -p /var/log/nginx \
    && mkdir -p /var/lib/emby/media \
    && mkdir -p /etc/nginx/conf.d \
    && mkdir -p /usr/local/scripts

# 复制Nginx配置文件
COPY nginx-statistics.conf /etc/nginx/conf.d/default.conf
COPY nginx.conf /etc/nginx/nginx.conf

# 复制日志解析脚本
COPY parse_nginx_logs.py /usr/local/scripts/
RUN chmod +x /usr/local/scripts/parse_nginx_logs.py

# 复制启动脚本
COPY start-nginx.sh /usr/local/bin/
RUN chmod +x /usr/local/bin/start-nginx.sh

# 创建crontab任务文件
RUN echo "*/5 * * * * /usr/bin/python3 /usr/local/scripts/parse_nginx_logs.py /var/log/nginx/download_stats.log" > /etc/crontabs/root

# 暴露端口
EXPOSE 80 443

# 设置环境变量
ENV EMBY_SERVER_HOST=emby-server
ENV EMBY_SERVER_PORT=8096
ENV SPRING_BOOT_HOST=emby-balance
ENV SPRING_BOOT_PORT=8080
ENV MEDIA_ROOT=/var/lib/emby/media

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD curl -f http://localhost/health || exit 1

# 启动命令
CMD ["/usr/local/bin/start-nginx.sh"]