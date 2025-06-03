#!/bin/bash

# Emby Balance Docker构建和部署脚本

set -e  # 遇到错误立即退出

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 日志函数
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检查依赖
check_dependencies() {
    log_info "检查依赖..."
    
    if ! command -v docker &> /dev/null; then
        log_error "Docker未安装，请先安装Docker"
        exit 1
    fi
    
    if ! command -v docker-compose &> /dev/null; then
        log_error "Docker Compose未安装，请先安装Docker Compose"
        exit 1
    fi
    
    log_success "依赖检查通过"
}

# 检查项目结构
check_project_structure() {
    log_info "检查项目结构..."
    
    required_files=(
        "docker-compose.yml"
        "docker/nginx-download/Dockerfile"
        "docker/emby-balance/Dockerfile"
        "docker/mysql/init.sql"
        "src/main/resources/application-docker.yml"
    )
    
    for file in "${required_files[@]}"; do
        if [ ! -f "$file" ]; then
            log_error "缺少必要文件: $file"
            exit 1
        fi
    done
    
    log_success "项目结构检查通过"
}

# 构建镜像
build_images() {
    log_info "开始构建Docker镜像..."
    
    # 构建nginx-download镜像
    log_info "构建nginx-download镜像..."
    docker build -t emby-balance/nginx-download:latest -f docker/nginx-download/Dockerfile .
    
    # 构建emby-balance镜像
    log_info "构建emby-balance镜像..."
    docker build -t emby-balance/emby-balance:latest -f docker/emby-balance/Dockerfile .
    
    log_success "镜像构建完成"
}

# 停止现有服务
stop_services() {
    log_info "停止现有服务..."
    
    if docker-compose ps | grep -q "Up"; then
        docker-compose down
        log_success "服务已停止"
    else
        log_info "没有运行中的服务"
    fi
}

# 清理旧镜像
cleanup_images() {
    log_info "清理旧镜像..."
    
    # 删除悬空镜像
    if docker images -f "dangling=true" -q | grep -q .; then
        docker rmi $(docker images -f "dangling=true" -q)
        log_success "悬空镜像已清理"
    else
        log_info "没有悬空镜像需要清理"
    fi
}

# 启动服务
start_services() {
    log_info "启动服务..."
    
    # 使用docker-compose启动服务
    docker-compose up -d
    
    log_success "服务启动完成"
}

# 等待服务就绪
wait_for_services() {
    log_info "等待服务就绪..."
    
    # 等待MySQL就绪
    log_info "等待MySQL服务..."
    timeout=60
    while [ $timeout -gt 0 ]; do
        if docker-compose exec -T mysql mysqladmin ping -h localhost --silent; then
            log_success "MySQL服务就绪"
            break
        fi
        sleep 2
        timeout=$((timeout-2))
    done
    
    if [ $timeout -le 0 ]; then
        log_error "MySQL服务启动超时"
        return 1
    fi
    
    # 等待Spring Boot应用就绪
    log_info "等待Spring Boot应用..."
    timeout=120
    while [ $timeout -gt 0 ]; do
        if curl -f http://localhost:8080/actuator/health &>/dev/null; then
            log_success "Spring Boot应用就绪"
            break
        fi
        sleep 3
        timeout=$((timeout-3))
    done
    
    if [ $timeout -le 0 ]; then
        log_error "Spring Boot应用启动超时"
        return 1
    fi
    
    # 等待Nginx服务就绪
    log_info "等待Nginx服务..."
    timeout=30
    while [ $timeout -gt 0 ]; do
        if curl -f http://localhost/health &>/dev/null && curl -f http://localhost:8081/health &>/dev/null; then
            log_success "Nginx服务就绪"
            break
        fi
        sleep 2
        timeout=$((timeout-2))
    done
    
    if [ $timeout -le 0 ]; then
        log_error "Nginx服务启动超时"
        return 1
    fi
}

# 验证部署
verify_deployment() {
    log_info "验证部署..."
    
    # 检查服务状态
    log_info "检查服务状态..."
    docker-compose ps
    
    # 测试API端点
    log_info "测试API端点..."
    
    endpoints=(
        "http://localhost:8080/actuator/health"
        "http://localhost/health"
        "http://localhost:8081/health"
        "http://localhost:8080/api/routes"
        "http://localhost:8080/api/performance/overview"
    )
    
    for endpoint in "${endpoints[@]}"; do
        if curl -f "$endpoint" &>/dev/null; then
            log_success "✓ $endpoint"
        else
            log_error "✗ $endpoint"
        fi
    done
    
    log_success "部署验证完成"
}

# 显示部署信息
show_deployment_info() {
    log_info "部署信息:"
    echo ""
    echo "🌐 服务访问地址:"
    echo "   - Emby代理服务: http://localhost"
    echo "   - Spring Boot API: http://localhost:8080"
    echo "   - 下载服务: http://localhost:8081"
    echo "   - Emby服务器: http://localhost:8096 (如果启用)"
    echo ""
    echo "📊 管理接口:"
    echo "   - 健康检查: http://localhost:8080/actuator/health"
    echo "   - 路由管理: http://localhost:8080/api/routes"
    echo "   - 性能监控: http://localhost:8080/api/performance/overview"
    echo ""
    echo "🔧 管理命令:"
    echo "   - 查看日志: docker-compose logs -f"
    echo "   - 重启服务: docker-compose restart"
    echo "   - 停止服务: docker-compose down"
    echo ""
    echo "📁 数据目录:"
    echo "   - 应用数据: docker volume ls | grep emby-balance"
    echo "   - 数据库: docker volume ls | grep mysql"
    echo ""
}

# 主函数
main() {
    log_info "开始Emby Balance Docker部署..."
    
    # 解析命令行参数
    SKIP_BUILD=false
    SKIP_CLEANUP=false
    FORCE_REBUILD=false
    
    while [[ $# -gt 0 ]]; do
        case $1 in
            --skip-build)
                SKIP_BUILD=true
                shift
                ;;
            --skip-cleanup)
                SKIP_CLEANUP=true
                shift
                ;;
            --force-rebuild)
                FORCE_REBUILD=true
                shift
                ;;
            --help)
                echo "用法: $0 [选项]"
                echo "选项:"
                echo "  --skip-build     跳过镜像构建"
                echo "  --skip-cleanup   跳过镜像清理"
                echo "  --force-rebuild  强制重新构建"
                echo "  --help           显示帮助信息"
                exit 0
                ;;
            *)
                log_error "未知选项: $1"
                exit 1
                ;;
        esac
    done
    
    # 执行部署步骤
    check_dependencies
    check_project_structure
    
    if [ "$SKIP_BUILD" = false ]; then
        if [ "$FORCE_REBUILD" = true ]; then
            stop_services
            docker-compose build --no-cache
        else
            build_images
        fi
    fi
    
    stop_services
    
    if [ "$SKIP_CLEANUP" = false ]; then
        cleanup_images
    fi
    
    start_services
    wait_for_services
    verify_deployment
    show_deployment_info
    
    log_success "Emby Balance部署完成！"
}

# 错误处理
trap 'log_error "部署过程中发生错误，请检查日志"' ERR

# 执行主函数
main "$@"