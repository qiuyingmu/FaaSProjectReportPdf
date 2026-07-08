# ═══════════════════════════════════════════════════════════
#  FaaSProjectReportPdf 部署手册
#
#  部署方式与 RealTimeVideo 完全一致：
#    - 后端：docker-compose 运行 Spring Boot
#    - 前端：宝塔 HTML 项目 + Nginx 反向代理
#    - 端口 8082（与 RealTimeVideo 的 8081 不冲突）
# ═══════════════════════════════════════════════════════════

# ┌─────────────────────────────────────────────────────────┐
# │  第 1 步：本地构建（你的电脑上操作）                      │
# └─────────────────────────────────────────────────────────┘
#
#   # 编译后端
#   mvn clean package -DskipTests
#
#   # 编译前端
#   cd frontend && npm run build && cd ..
#
#   产物在 deploy/ 目录中已准备好：
#     deploy/
#     ├── report-springboot-1.0.0.jar   # 后端 JAR
#     ├── Dockerfile.backend             # Docker 镜像定义
#     ├── docker-compose.yml             # 容器编排
#     ├── .env.example                   # 环境变量模板
#     ├── frontend-dist/                 # 前端静态文件
#     ├── nginx.conf                     # Nginx 配置参考
#     └── DEPLOY.md                      # 本文档
#
# ┌─────────────────────────────────────────────────────────┐
# │  第 2 步：服务器 SSH 操作                                │
# └─────────────────────────────────────────────────────────┘
#
#   2.1 创建项目目录
#       mkdir -p /www/wwwroot/report.jgjl.cn
#       cd /www/wwwroot/report.jgjl.cn
#
#   2.2 上传以下文件到 /www/wwwroot/report.jgjl.cn/
#       - docker-compose.yml
#       - Dockerfile.backend
#       - report-springboot-1.0.0.jar
#       - .env（基于 .env.example 修改）
#
#   2.3 创建数据目录（H2 持久化 + 日志）
#       mkdir -p data logs
#
#   2.4 启动后端
#       docker compose up -d
#
#   2.5 验证后端
#       curl http://127.0.0.1:8082/actuator/health
#       → {"status":"UP"}
#
# ┌─────────────────────────────────────────────────────────┐
# │  第 3 步：宝塔面板操作                                   │
# └─────────────────────────────────────────────────────────┘
#
#   3.1 添加站点
#       左侧菜单 → 网站 → 添加站点
#       域名: report.jgjl.cn
#       类型: HTML 项目
#       根目录: /www/wwwroot/report.jgjl.cn
#       提交创建
#
#   3.2 上传前端文件
#       文件管理 → 进入 /www/wwwroot/report.jgjl.cn/
#       上传 frontend-dist/ 下所有文件（index.html、assets/ 等）
#
#   3.3 配置 SSL 证书
#       站点设置 → SSL → Let's Encrypt → 申请
#
#   3.4 修改 Nginx 配置
#       站点设置 → 配置文件
#       全选替换为 deploy/nginx.conf 的内容
#       （注意把 report.jgjl.cn 改成你的实际域名）
#
#   3.5 重启 Nginx
#       在宝塔面板顶部 → 服务 → Nginx → 重启
#
# ┌─────────────────────────────────────────────────────────┐
# │  第 4 步：验证                                          │
# └─────────────────────────────────────────────────────────┘
#
#   浏览器打开 https://report.jgjl.cn
#   默认账号: admin / admin123
#
#   ✅ 登录成功 → 部署完成
#   ⚠️ 部署后请立即修改管理员密码
#
# ═══════════════════════════════════════════════════════════
#  与 RealTimeVideo 对比
# ═══════════════════════════════════════════════════════════
#
#  项目              域名                          后端端口  数据库
#  RealTimeVideo    realtimevideo.jgjl.cn          8081      MySQL（docker）
#  FaaSReportPdf    report.jgjl.cn                 8082      H2 嵌入式（无需 docker）
#
#  两个项目在各自的子域名下独立运行，互不影响。
#  后端端口不同（8081 vs 8082），Nginx 配置独立。
# ═══════════════════════════════════════════════════════════
