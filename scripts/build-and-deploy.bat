@echo off
setlocal enabledelayedexpansion

REM Emby Balance Docker构建和部署脚本 (Windows版本)

REM 颜色定义
set "RED=[91m"
set "GREEN=[92m"
set "YELLOW=[93m"
set "BLUE=[94m"
set "NC=[0m"

REM 日志函数
:log_info
echo %BLUE%[INFO]%NC% %~1
goto :eof

:log_success
echo %GREEN%[SUCCESS]%NC% %~1
goto :eof

:log_warning
echo %YELLOW%[WARNING]%NC% %~1
goto :eof

:log_error
echo %RED%[ERROR]%NC% %~1
goto :eof

REM 检查依赖
:check_dependencies
call :log_info "检查依赖..."

docker --version >nul 2>&1
if errorlevel 1 (
    call :log_error "Docker未安装，请先安装Docker Desktop"
    exit /b 1
)

docker-compose --version >nul 2>&1
if errorlevel 1 (
    call :log_error "Docker Compose未安装，请先安装Docker Compose"
    exit /b 1
)

call :log_success "依赖检查通过"
goto :eof

REM 检查项目结构
:check_project_structure
call :log_info "检查项目结构..."

set "required_files=docker-compose.yml docker\nginx-download\Dockerfile docker\emby-balance\Dockerfile docker\mysql\init.sql src\main\resources\application-docker.yml"

for %%f in (%required_files%) do (
    if not exist "%%f" (
        call :log_error "缺少必要文件: %%f"
        exit /b 1
    )
)

call :log_success "项目结构检查通过"
goto :eof

REM 构建镜像
:build_images
call :log_info "开始构建Docker镜像..."

REM 构建nginx-download镜像
call :log_info "构建nginx-download镜像..."
docker build -t emby-balance/nginx-download:latest -f docker/nginx-download/Dockerfile .
if errorlevel 1 (
    call :log_error "nginx-download镜像构建失败"
    exit /b 1
)

REM 构建emby-balance镜像
call :log_info "构建emby-balance镜像..."
docker build -t emby-balance/emby-balance:latest -f docker/emby-balance/Dockerfile .
if errorlevel 1 (
    call :log_error "emby-balance镜像构建失败"
    exit /b 1
)

call :log_success "镜像构建完成"
goto :eof

REM 停止现有服务
:stop_services
call :log_info "停止现有服务..."

docker-compose ps | findstr "Up" >nul 2>&1
if not errorlevel 1 (
    docker-compose down
    call :log_success "服务已停止"
) else (
    call :log_info "没有运行中的服务"
)
goto :eof

REM 清理旧镜像
:cleanup_images
call :log_info "清理旧镜像..."

for /f "tokens=*" %%i in ('docker images -f "dangling=true" -q 2^>nul') do (
    docker rmi %%i >nul 2>&1
)

call :log_success "悬空镜像已清理"
goto :eof

REM 启动服务
:start_services
call :log_info "启动服务..."

docker-compose up -d
if errorlevel 1 (
    call :log_error "服务启动失败"
    exit /b 1
)

call :log_success "服务启动完成"
goto :eof

REM 等待服务就绪
:wait_for_services
call :log_info "等待服务就绪..."

REM 等待MySQL就绪
call :log_info "等待MySQL服务..."
set /a timeout=60
:wait_mysql
if %timeout% leq 0 (
    call :log_error "MySQL服务启动超时"
    exit /b 1
)
docker-compose exec -T mysql mysqladmin ping -h localhost --silent >nul 2>&1
if not errorlevel 1 (
    call :log_success "MySQL服务就绪"
    goto :wait_spring
)
timeout /t 2 /nobreak >nul
set /a timeout-=2
goto :wait_mysql

:wait_spring
REM 等待Spring Boot应用就绪
call :log_info "等待Spring Boot应用..."
set /a timeout=120
:wait_spring_loop
if %timeout% leq 0 (
    call :log_error "Spring Boot应用启动超时"
    exit /b 1
)
curl -f http://localhost:8080/actuator/health >nul 2>&1
if not errorlevel 1 (
    call :log_success "Spring Boot应用就绪"
    goto :wait_nginx
)
timeout /t 3 /nobreak >nul
set /a timeout-=3
goto :wait_spring_loop

:wait_nginx
REM 等待Nginx服务就绪
call :log_info "等待Nginx服务..."
set /a timeout=30
:wait_nginx_loop
if %timeout% leq 0 (
    call :log_error "Nginx服务启动超时"
    exit /b 1
)
curl -f http://localhost/health >nul 2>&1 && curl -f http://localhost:8081/health >nul 2>&1
if not errorlevel 1 (
    call :log_success "Nginx服务就绪"
    goto :eof
)
timeout /t 2 /nobreak >nul
set /a timeout-=2
goto :wait_nginx_loop

REM 验证部署
:verify_deployment
call :log_info "验证部署..."

REM 检查服务状态
call :log_info "检查服务状态..."
docker-compose ps

REM 测试API端点
call :log_info "测试API端点..."

set "endpoints=http://localhost:8080/actuator/health http://localhost/health http://localhost:8081/health http://localhost:8080/api/routes http://localhost:8080/api/performance/overview"

for %%e in (%endpoints%) do (
    curl -f "%%e" >nul 2>&1
    if not errorlevel 1 (
        call :log_success "✓ %%e"
    ) else (
        call :log_error "✗ %%e"
    )
)

call :log_success "部署验证完成"
goto :eof

REM 显示部署信息
:show_deployment_info
call :log_info "部署信息:"
echo.
echo 🌐 服务访问地址:
echo    - Emby代理服务: http://localhost
echo    - Spring Boot API: http://localhost:8080
echo    - 下载服务: http://localhost:8081
echo    - Emby服务器: http://localhost:8096 (如果启用)
echo.
echo 📊 管理接口:
echo    - 健康检查: http://localhost:8080/actuator/health
echo    - 路由管理: http://localhost:8080/api/routes
echo    - 性能监控: http://localhost:8080/api/performance/overview
echo.
echo 🔧 管理命令:
echo    - 查看日志: docker-compose logs -f
echo    - 重启服务: docker-compose restart
echo    - 停止服务: docker-compose down
echo.
echo 📁 数据目录:
echo    - 应用数据: docker volume ls ^| findstr emby-balance
echo    - 数据库: docker volume ls ^| findstr mysql
echo.
goto :eof

REM 主函数
:main
call :log_info "开始Emby Balance Docker部署..."

REM 解析命令行参数
set "SKIP_BUILD=false"
set "SKIP_CLEANUP=false"
set "FORCE_REBUILD=false"

:parse_args
if "%~1"=="" goto :start_deployment
if "%~1"=="--skip-build" (
    set "SKIP_BUILD=true"
    shift
    goto :parse_args
)
if "%~1"=="--skip-cleanup" (
    set "SKIP_CLEANUP=true"
    shift
    goto :parse_args
)
if "%~1"=="--force-rebuild" (
    set "FORCE_REBUILD=true"
    shift
    goto :parse_args
)
if "%~1"=="--help" (
    echo 用法: %0 [选项]
    echo 选项:
    echo   --skip-build     跳过镜像构建
    echo   --skip-cleanup   跳过镜像清理
    echo   --force-rebuild  强制重新构建
    echo   --help           显示帮助信息
    exit /b 0
)
call :log_error "未知选项: %~1"
exit /b 1

:start_deployment
REM 执行部署步骤
call :check_dependencies
if errorlevel 1 exit /b 1

call :check_project_structure
if errorlevel 1 exit /b 1

if "%SKIP_BUILD%"=="false" (
    if "%FORCE_REBUILD%"=="true" (
        call :stop_services
        docker-compose build --no-cache
    ) else (
        call :build_images
        if errorlevel 1 exit /b 1
    )
)

call :stop_services

if "%SKIP_CLEANUP%"=="false" (
    call :cleanup_images
)

call :start_services
if errorlevel 1 exit /b 1

call :wait_for_services
if errorlevel 1 exit /b 1

call :verify_deployment
call :show_deployment_info

call :log_success "Emby Balance部署完成！"
goto :eof

REM 执行主函数
call :main %*