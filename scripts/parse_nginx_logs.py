#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Nginx下载日志解析脚本
用于解析Nginx访问日志并将统计数据发送到Spring Boot应用

使用方法:
python parse_nginx_logs.py /var/log/nginx/download_stats.log

或者设置为定时任务:
# 每5分钟执行一次
*/5 * * * * /usr/bin/python3 /path/to/parse_nginx_logs.py /var/log/nginx/download_stats.log
"""

import re
import sys
import json
import requests
import os
from datetime import datetime
from urllib.parse import parse_qs, urlparse
import time

# Spring Boot应用地址
SPRING_BOOT_URL = "http://127.0.0.1:8080/api/statistics/record/batch"

# 日志格式正则表达式
# 对应nginx配置中的log_format download_stats
LOG_PATTERN = re.compile(
    r'(?P<remote_addr>\S+) - (?P<remote_user>\S+) \[(?P<time_local>[^\]]+)\] '
    r'"(?P<request>[^"]+)" (?P<status>\d+) (?P<bytes_sent>\d+) '
    r'"(?P<http_referer>[^"]+)" "(?P<http_user_agent>[^"]+)" '
    r'(?P<request_time>[\d\.]+) (?P<upstream_response_time>[\d\.-]+) '
    r'(?P<request_length>\d+) (?P<body_bytes_sent>\d+)'
)

# 记录已处理的日志位置
POSITION_FILE = "/tmp/nginx_log_position.txt"

def get_last_position(log_file):
    """获取上次处理的日志位置"""
    position_file = f"{POSITION_FILE}.{os.path.basename(log_file)}"
    try:
        if os.path.exists(position_file):
            with open(position_file, 'r') as f:
                return int(f.read().strip())
    except:
        pass
    return 0

def save_position(log_file, position):
    """保存当前处理的日志位置"""
    position_file = f"{POSITION_FILE}.{os.path.basename(log_file)}"
    try:
        with open(position_file, 'w') as f:
            f.write(str(position))
    except Exception as e:
        print(f"保存位置失败: {e}")

def parse_request(request_line):
    """解析HTTP请求行"""
    try:
        parts = request_line.split(' ')
        if len(parts) >= 2:
            method = parts[0]
            url = parts[1]
            return method, url
    except:
        pass
    return None, None

def extract_route_id_and_path(url):
    """从URL中提取路由ID和文件路径"""
    try:
        parsed = urlparse(url)
        if parsed.path == '/emby_download':
            query_params = parse_qs(parsed.query)
            route_id = query_params.get('route_id', [None])[0]
            file_path = query_params.get('path', [None])[0]
            return route_id, file_path
    except:
        pass
    return None, None

def parse_log_line(line):
    """解析单行日志"""
    match = LOG_PATTERN.match(line.strip())
    if not match:
        return None
    
    data = match.groupdict()
    
    # 解析请求
    method, url = parse_request(data['request'])
    if not url or not url.startswith('/emby_download'):
        return None  # 只处理下载请求
    
    # 提取路由ID和文件路径
    route_id, file_path = extract_route_id_and_path(url)
    if not route_id:
        return None
    
    # 计算下载时长（毫秒）
    try:
        request_time = float(data['request_time']) * 1000  # 转换为毫秒
    except:
        request_time = 0
    
    # 计算响应时间（毫秒）
    try:
        upstream_response_time = data['upstream_response_time']
        if upstream_response_time == '-':
            response_time = 0
        else:
            response_time = float(upstream_response_time) * 1000
    except:
        response_time = 0
    
    # 构建统计数据
    stats = {
        'routeId': int(route_id),
        'clientIp': data['remote_addr'],
        'filePath': file_path,
        'downloadSize': int(data['body_bytes_sent']),
        'downloadDuration': int(request_time),
        'responseTime': int(response_time),
        'success': data['status'] == '200',
        'userAgent': data['http_user_agent']
    }
    
    return stats

def send_statistics(statistics_list):
    """发送统计数据到Spring Boot应用"""
    if not statistics_list:
        return True
    
    try:
        payload = {
            'statistics': statistics_list
        }
        
        response = requests.post(
            SPRING_BOOT_URL,
            json=payload,
            headers={'Content-Type': 'application/json'},
            timeout=10
        )
        
        if response.status_code == 200:
            print(f"成功发送 {len(statistics_list)} 条统计数据")
            return True
        else:
            print(f"发送统计数据失败: HTTP {response.status_code} - {response.text}")
            return False
            
    except Exception as e:
        print(f"发送统计数据异常: {e}")
        return False

def process_log_file(log_file):
    """处理日志文件"""
    if not os.path.exists(log_file):
        print(f"日志文件不存在: {log_file}")
        return
    
    # 获取上次处理位置
    last_position = get_last_position(log_file)
    
    statistics_list = []
    current_position = last_position
    
    try:
        with open(log_file, 'r', encoding='utf-8') as f:
            # 跳转到上次处理位置
            f.seek(last_position)
            
            for line in f:
                current_position = f.tell()
                
                # 解析日志行
                stats = parse_log_line(line)
                if stats:
                    statistics_list.append(stats)
                
                # 批量发送（每100条）
                if len(statistics_list) >= 100:
                    if send_statistics(statistics_list):
                        save_position(log_file, current_position)
                        statistics_list = []
                    else:
                        # 发送失败，停止处理
                        break
            
            # 发送剩余的统计数据
            if statistics_list:
                if send_statistics(statistics_list):
                    save_position(log_file, current_position)
                    
    except Exception as e:
        print(f"处理日志文件失败: {e}")

def main():
    if len(sys.argv) != 2:
        print("使用方法: python parse_nginx_logs.py <log_file>")
        print("示例: python parse_nginx_logs.py /var/log/nginx/download_stats.log")
        sys.exit(1)
    
    log_file = sys.argv[1]
    
    print(f"开始处理日志文件: {log_file}")
    print(f"Spring Boot应用地址: {SPRING_BOOT_URL}")
    
    process_log_file(log_file)
    
    print("日志处理完成")

if __name__ == '__main__':
    main()