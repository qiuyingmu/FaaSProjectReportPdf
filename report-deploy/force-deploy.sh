#!/bin/bash
# 强制重新部署脚本 —— 避免 Docker 缓存老 JAR
# 使用方法：在 /opt/report 目录下执行 bash force-deploy.sh

set -e

echo "==========================================="
echo "  强制重新部署运营报告系统"
echo "==========================================="

# 1. 停掉所有相关容器
echo ""
echo "▶ 第 1 步：停掉旧容器..."
docker compose down
docker stop report-backend 2>/dev/null || true
docker stop report-db 2>/dev/null || true

# 2. 删除所有可能的旧镜像（确保重新构建）
echo ""
echo "▶ 第 2 步：删除所有可能的旧镜像..."
docker rmi report-backend 2>/dev/null || true
docker rmi $(docker images | grep "report" | awk '{print $3}') 2>/dev/null || true
docker image prune -f

# 3. 重新构建并启动
echo ""
echo "▶ 第 3 步：重新构建并启动..."
docker compose up -d --build

# 4. 等待并验证
echo ""
echo "▶ 第 4 步：等待服务启动..."
sleep 20

# 5. 验证 JAR 是不是新代码
echo ""
echo "==========================================="
echo "  验证新代码是否真的在容器内运行"
echo "==========================================="
echo ""

# 5.1 检查容器内 JAR 的修改时间
echo "▶ 容器内 JAR 修改时间："
docker exec report-backend sh -c "ls -la /app/app.jar 2>/dev/null || ls -la /app.jar 2>/dev/null || echo 'JAR 路径未找到'"

echo ""
echo "▶ 宿主机 JAR 修改时间（应该和上面的接近）："
ls -la report-springboot-1.0.0.jar

echo ""
echo "▶ 容器内 ReportHtmlBuilder.class 是否包含 rangeToPeriodLabel 调用："
docker exec report-backend sh -c "unzip -p /app/app.jar BOOT-INF/classes/com/alibaba/work/faas/report/ReportHtmlBuilder.class 2>/dev/null | strings | grep -E 'rangeToPeriodLabel|formatRangeLabel' | head -5"

echo ""
echo "▶ 容器内 PlatformReportStrategy.class 是否调用 formatRangeLabel："
docker exec report-backend sh -c "unzip -p /app/app.jar BOOT-INF/classes/com/alibaba/work/faas/report/strategy/PlatformReportStrategy.class 2>/dev/null | strings | grep -E 'formatRangeLabel' | head -5"

echo ""
echo "▶ 【关键】容器内 rangeToPeriodLabel 处理 lastMonth 的逻辑："
docker exec report-backend sh -c "unzip -p /app/app.jar BOOT-INF/classes/com/alibaba/work/faas/report/ReportDateUtils.class 2>/dev/null | strings | grep -E 'lastMonth|lastWeek|lastQuarter|month|week|quarter' | head -10"

echo ""
echo "==========================================="
echo "  部署完成！"
echo "==========================================="
echo ""
echo "▶ 容器时间（应显示 CST 北京时间）："
docker exec report-backend date

echo ""
echo "▶ 调度器状态（应显示 3 个任务 🟢）："
docker logs report-backend --tail 30 | grep -E "周报|月报|季报" | head -5

echo ""
echo "▶ 测试 API："
curl -s http://localhost:8082/report/api/test
echo ""

echo ""
echo "==========================================="
echo "  ✅ 部署完成！"
echo "  📌 如果还有问题，请查看完整日志："
echo "      docker logs report-backend --tail 100"
echo "==========================================="
