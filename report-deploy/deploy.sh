#!/bin/bash
# ═══════════════════════════════════════════════════════════
#  Report Deploy — 一键部署脚本
#  用法：
#     1. 编辑 .env，填入真实值（密码、钉钉凭证等）
#     2. 上传 report-deploy.tar.gz 到服务器 /opt/
#     3. SSH 执行：
#        cd /opt && mkdir -p report && tar -xzf report-deploy.tar.gz -C report && cd report && chmod +x deploy.sh && ./deploy.sh
# ═══════════════════════════════════════════════════════════

set -e

DEPLOY_DIR="/opt/report"
FRONTEND_TARGET="/www/wwwroot/realtimevideo.jgjl.cn/report"
BACKEND_PORT=8082

echo "=========================================="
echo "  运营报告系统 — 一键部署"
echo "=========================================="
echo ""

# ── 1. 确认当前目录 ──
if [ "$(pwd)" != "$DEPLOY_DIR" ]; then
    echo "❌ 请在 $DEPLOY_DIR 目录下执行"
    echo "   请先运行: cd $DEPLOY_DIR"
    exit 1
fi

# ── 2. 检查必需文件 ──
REQUIRED_FILES=("docker-compose.yml" "Dockerfile.backend" "report-springboot-1.0.0.jar" ".env")
for f in "${REQUIRED_FILES[@]}"; do
    if [ ! -f "$f" ]; then
        echo "❌ 缺少文件: $f"
        exit 1
    fi
done
echo "✅ 文件完整性检查通过"

# ── 3. 检查 .env 是否已修改（排除占位符） ──
echo ""
echo "🔍 检查 .env 配置..."
CHECK_FAILED=0

check_env() {
    local key=$1
    local placeholder=$2
    local value=$(grep "^${key}=" .env | head -1 | cut -d'=' -f2-)
    if [ -z "$value" ] || [ "$value" = "$placeholder" ]; then
        echo "  ❌ $key 未修改（当前值: ${value:-空}）"
        CHECK_FAILED=1
    else
        echo "  ✅ $key 已填写"
    fi
}

check_env "REPORT_ADMIN_PASSWORD" "your-strong-password-here"
# DB_PASSWORD 默认 Report@2026 可直接使用
echo "  ✅ DB_PASSWORD 使用默认值"
# CORS_ALLOWED_ORIGINS 默认 https://realtimevideo.jgjl.cn 即本域名
echo "  ✅ CORS_ALLOWED_ORIGINS 使用默认值"
check_env "dingtalk.app.key" "your-app-key-here"
check_env "dingtalk.app.secret" "your-app-secret-here"

if [ $CHECK_FAILED -eq 1 ]; then
    echo ""
    echo "=========================================="
    echo "  ❌ 请先编辑 .env 文件，填入真实值后重试"
    echo "=========================================="
    echo "  vi .env"
    echo "  然后重新运行: ./deploy.sh"
    echo "=========================================="
    exit 1
fi
echo "✅ .env 配置检查通过"

# ── 3. 拉取 PostgreSQL 镜像（避免启动时等待） ──
echo ""
echo "📥 拉取 PostgreSQL 镜像..."
docker pull postgres:15 -q
echo "✅ PostgreSQL 镜像就绪"

# ── 4. 启动所有容器 ──
echo ""
echo "🚀 启动服务 (PostgreSQL + 后端, 端口 $BACKEND_PORT)..."
docker compose up -d
echo "⏳ 等待数据库就绪..."
sleep 5

# 验证后端
HEALTH=$(curl -sf http://127.0.0.1:$BACKEND_PORT/actuator/health 2>/dev/null || echo "")
if echo "$HEALTH" | grep -q "UP"; then
    echo "✅ 后端启动成功: http://127.0.0.1:$BACKEND_PORT/actuator/health"
else
    echo "⚠️  后端启动可能未完成，请稍后手动检查: curl http://127.0.0.1:$BACKEND_PORT/actuator/health"
fi

# ── 5. 部署前端 ──
echo ""
echo "📦 部署前端文件..."
mkdir -p "$FRONTEND_TARGET"
cp -r frontend/* "$FRONTEND_TARGET/"
echo "✅ 前端已部署到: $FRONTEND_TARGET"
echo "   访问地址: https://realtimevideo.jgjl.cn/report/"

echo ""
echo "=========================================="
echo "  ✅ 部署完成！"
echo "=========================================="
echo "  后端: 127.0.0.1:$BACKEND_PORT (docker)"
echo "  前端: https://realtimevideo.jgjl.cn/report/"
echo "  登录: admin / .env 中设置的密码"
echo ""
echo "  ⚠️  最后一步："
echo "  请到宝塔 → 站点设置 → realtimevideo.jgjl.cn"
echo "  → 配置文件，在 location / { ... } 上面"
echo "  插入 nginx-subpath.conf 的内容。"
echo "=========================================="
