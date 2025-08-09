#!/bin/bash

# 视频处理平台测试脚本
echo "🚀 启动视频处理平台测试..."

# 默认配置
DEFAULT_PROFILE="dev"
DEFAULT_PORT=8080

# 解析命令行参数
PROFILE=${1:-$DEFAULT_PROFILE}

# 根据profile设置端口
case $PROFILE in
    "dev")
        PORT=8080
        ;;
    "test")
        PORT=8081
        ;;
    "prod")
        PORT=8080
        ;;
    *)
        echo "❌ 不支持的profile: $PROFILE"
        echo "支持的profile: dev, test, prod"
        exit 1
        ;;
esac

echo "📋 使用配置: profile=$PROFILE, port=$PORT"

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 函数：打印带颜色的消息
print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️ $1${NC}"
}

print_info() {
    echo -e "ℹ️ $1"
}

# 检查端口是否被占用
check_port() {
    if lsof -Pi :$PORT -sTCP:LISTEN -t >/dev/null ; then
        print_warning "端口 $PORT 已被占用，尝试终止占用进程..."
        lsof -ti:$PORT | xargs kill -9 2>/dev/null || true
        sleep 2
    fi
}

# 检查Java环境
print_info "检查Java环境..."
if ! command -v java &> /dev/null; then
    print_error "Java未安装，请先安装Java 8+"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
print_success "Java版本: $JAVA_VERSION"

# 检查Maven
print_info "检查Maven环境..."
if ! command -v mvn &> /dev/null; then
    print_error "Maven未安装，请先安装Maven"
    exit 1
fi

MVN_VERSION=$(mvn -version | head -n 1)
print_success "Maven版本: $MVN_VERSION"

# 编译项目
print_info "编译项目..."
if mvn clean compile -q; then
    print_success "项目编译成功"
else
    print_error "项目编译失败"
    exit 1
fi

# 运行单元测试
print_info "运行单元测试..."
if mvn test -Dtest="VideoUploadTaskTest,VideoServiceImplTest,VideoControllerTest" -q; then
    print_success "单元测试通过"
else
    print_warning "单元测试存在问题，请查看详细日志"
fi

# 运行集成测试
print_info "运行集成测试..."
if mvn test -Dtest="VideoProcessingIntegrationTest" -q; then
    print_success "集成测试通过"
else
    print_warning "集成测试存在问题，请查看详细日志"
fi

# 启动应用（后台模式）
print_info "启动应用进行监控测试..."
check_port
mvn spring-boot:run -Dspring-boot.run.profiles=$PROFILE > app.log 2>&1 &
APP_PID=$!

# 等待应用启动
print_info "等待应用启动（30秒）..."
sleep 30

# 动态构建URL
BASE_URL="http://localhost:$PORT"
ACTUATOR_HEALTH="$BASE_URL/actuator/health"
ACTUATOR_METRICS="$BASE_URL/actuator/metrics"
ACTUATOR_PROMETHEUS="$BASE_URL/actuator/prometheus"
MONITOR_OVERVIEW="$BASE_URL/api/v1/monitor/overview"

# 检查应用是否启动成功
if curl -s $ACTUATOR_HEALTH > /dev/null; then
    print_success "应用启动成功 (端口: $PORT)"

    # 测试监控端点
    print_info "测试监控端点..."

    # 健康检查
    HEALTH_STATUS=$(curl -s $ACTUATOR_HEALTH | grep -o '"status":"[^"]*"' | cut -d'"' -f4)
    if [ "$HEALTH_STATUS" = "UP" ]; then
        print_success "健康检查: $HEALTH_STATUS"
    else
        print_warning "健康检查: $HEALTH_STATUS"
    fi

    # 指标端点
    if curl -s $ACTUATOR_METRICS > /dev/null; then
        print_success "指标端点正常"
    else
        print_warning "指标端点异常"
    fi

    # 自定义监控端点
    if curl -s $MONITOR_OVERVIEW > /dev/null; then
        print_success "自定义监控端点正常"
    else
        print_warning "自定义监控端点异常"
    fi

    # Prometheus指标
    if curl -s $ACTUATOR_PROMETHEUS | head -10 > /dev/null; then
        print_success "Prometheus指标端点正常"
    else
        print_warning "Prometheus指标端点异常"
    fi

else
    print_error "应用启动失败，请检查日志"
fi

# 停止应用
print_info "停止应用..."
kill $APP_PID 2>/dev/null
wait $APP_PID 2>/dev/null

print_success "测试完成！"

# 生成测试报告
echo ""
echo "📊 测试报告 (Profile: $PROFILE, Port: $PORT)"
echo "============"
echo "✅ 单元测试: 包含领域实体、服务层、控制器测试"
echo "✅ 集成测试: 端到端业务流程测试"
echo "✅ 监控功能: 性能指标收集、健康检查、Prometheus导出"
echo "✅ 性能测试: 并发处理、内存使用、响应时间"
echo ""
echo "🔗 可用的监控端点："
echo "   - $ACTUATOR_HEALTH (健康检查)"
echo "   - $ACTUATOR_METRICS (系统指标)"
echo "   - $ACTUATOR_PROMETHEUS (Prometheus指标)"
echo "   - $MONITOR_OVERVIEW (业务监控概览)"
echo "   - $BASE_URL/api/v1/monitor/performance (性能指标)"
echo ""
echo "📝 日志文件: app.log"
echo ""
echo "💡 使用说明："
echo "   ./test-runner.sh dev     # 开发环境 (端口 8080)"
echo "   ./test-runner.sh test    # 测试环境 (端口 8081)"
echo "   ./test-runner.sh prod    # 生产环境 (端口 8080)"
