# ═══════════════════════════════════════════════════════════
#  FaaSProjectReportPdf 部署手册（子路径版）
#
#  没有独立域名？没问题！
#  部署在 realtimevideo.jgjl.cn/report/ 子路径下，
#  与 RealTimeVideo 共享同一个域名和 Nginx。
# ═══════════════════════════════════════════════════════════

# 最终访问地址：https://realtimevideo.jgjl.cn/report/
#
# 路径规划：
#   Frontend:  https://realtimevideo.jgjl.cn/report/       ← SPA
#   API:       https://realtimevideo.jgjl.cn/report/api/   ← 后端
#   Admin:     https://realtimevideo.jgjl.cn/report/admin/ ← 登录页
#
# Nginx 自动剥离 /report 前缀再转发到后端 8082 端口，
# 后端不需要改任何代码。

# ┌─────────────────────────────────────────────────────────┐
# │  第 1 步：服务器 SSH                                    │
# └─────────────────────────────────────────────────────────┘
#
#   1.1 创建报告系统目录（在 RealTimeVideo 站点目录下）
#       mkdir -p /www/wwwroot/realtimevideo.jgjl.cn/report
#       cd /www/wwwroot/realtimevideo.jgjl.cn/report
#
#   1.2 上传以下文件到 /www/wwwroot/realtimevideo.jgjl.cn/report/
#       - docker-compose.yml
#       - Dockerfile.backend
#       - report-springboot-1.0.0.jar
#       - .env（基于 .env.example 修改）
#
#   1.3 创建数据目录
#       mkdir -p data logs
#
#   1.4 启动后端
#       docker compose up -d
#
#   1.5 验证后端
#       curl http://127.0.0.1:8082/actuator/health
#       → {"status":"UP"}

# ┌─────────────────────────────────────────────────────────┐
# │  第 2 步：上传前端                                       │
# └─────────────────────────────────────────────────────────┘
#
#   宝塔文件管理 → 进入 /www/wwwroot/realtimevideo.jgjl.cn/
#   创建 report/ 目录
#   上传 frontend-dist/ 下所有文件到 report/ 目录
#   最终目录结构：
#
#     /www/wwwroot/realtimevideo.jgjl.cn/
#     ├── index.html               # RealTimeVideo 前端
#     ├── assets/                   # RealTimeVideo 资源
#     ├── ...                       # 其他 RealTimeVideo 文件
#     ├── report/                   # ← 报告系统
#     │   ├── index.html
#     │   ├── assets/
#     │   │   ├── index-xxx.js
#     │   │   └── index-xxx.css
#     │   ├── docker-compose.yml
#     │   ├── Dockerfile.backend
#     │   ├── .env
#     │   └── report-springboot-1.0.0.jar

# ┌─────────────────────────────────────────────────────────┐
# │  第 3 步：修改 Nginx 配置                                │
# └─────────────────────────────────────────────────────────┘
#
#   宝塔 → 站点设置 → realtimevideo.jgjl.cn → 配置文件
#   在 server {} 块内，找到 SPA 路由这一行：
#
#       location / { try_files $uri $uri/ /index.html; }
#
#   在它前面插入 nginx-subpath.conf 全部内容。
#   保存后 Nginx 自动重载。

# ┌─────────────────────────────────────────────────────────┐
# │  第 4 步：验证                                          │
# └─────────────────────────────────────────────────────────┘
#
#   浏览器打开 https://realtimevideo.jgjl.cn/report/
#   默认账号: admin / admin123
#
#   ✅ 登录成功 → 部署完成
#   ⚠️ 部署后请立即修改管理员密码

# ═══════════════════════════════════════════════════════════
#  两个应用互不影响的原理
# ═══════════════════════════════════════════════════════════
#
#  realtimevideo.jgjl.cn/        → RealTimeVideo SPA（原有的）
#  realtimevideo.jgjl.cn/api/    → RealTimeVideo 后端（8081）
#
#  realtimevideo.jgjl.cn/report/         → 报告系统 SPA
#  realtimevideo.jgjl.cn/report/api/     → Nginx 剥离 /report
#                                         → 报告系统后端 (8082)
#  realtimevideo.jgjl.cn/report/admin/   → Nginx 剥离 /report
#                                         → 报告系统后端 (8082)
#
#  Nginx 通过 rewrite 剥离 /report 前缀，后端以为自己在根路径。
#  JS/CSS 资源通过 --base=/report/ 编译，引用路径自动正确。
# ═══════════════════════════════════════════════════════════
