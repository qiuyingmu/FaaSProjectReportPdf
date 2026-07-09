# 运营报告系统 — 完整部署流程

## 最终访问地址

```
https://realtimevideo.jgjl.cn/report/
```

## 部署全程只需 5 步

---

### 第 1 步：上传压缩包

宝塔面板 → 文件管理 → 上传 `report-deploy.tar.gz` 到 **`/opt/`**

---

### 第 2 步：解压并修改 .env

```bash
# SSH 登录服务器，执行
cd /opt && mkdir -p report && tar -xzf report-deploy.tar.gz -C report && cd report
```

然后编辑 `.env` 文件，填入真实值（`vi .env` 或宝塔文件管理里编辑）：

```ini
REPORT_ADMIN_PASSWORD=你的管理员密码       # 登录后台用
DB_PASSWORD=你的数据库密码                   # PostgreSQL 密码
CORS_ALLOWED_ORIGINS=https://realtimevideo.jgjl.cn  # 不要改
dingtalk.app.key=你的钉钉AppKey
dingtalk.app.secret=你的钉钉AppSecret
```

---

### 第 3 步：运行部署脚本

```bash
chmod +x deploy.sh && ./deploy.sh
```

脚本会自动检查 `.env` 是否已填写，如果还是占位符会报错退出：

```
🔍 检查 .env 配置...
  ✅ REPORT_ADMIN_PASSWORD 已填写
  ❌ dingtalk.app.key 未修改（当前值: your-app-key-here）
  ❌ dingtalk.app.secret 未修改（当前值: your-app-secret-here）

==========================================
  ❌ 请先编辑 .env 文件，填入真实值后重试
==========================================
  vi .env
  然后重新运行: ./deploy.sh
==========================================
```

填好后再跑一次即可。检查通过后自动完成：

```
✅ .env 配置检查通过
📥 拉取 PostgreSQL 镜像...
🚀 启动服务 (PostgreSQL + 后端, 端口 8082)...
⏳ 等待数据库就绪...
✅ 后端启动成功
📦 部署前端文件...
✅ 部署完成！
```

---

### 第 4 步：宝塔改 Nginx 配置

宝塔 → 站点 → **realtimevideo.jgjl.cn** → 设置 → 配置文件

找到：

```nginx
location / { try_files $uri $uri/ /index.html; }
```

**在这行上面**，插入 `nginx-subpath.conf` 全部内容（也可手动复制下方代码）：

```nginx
# ──── 报告系统前端 ────
location /report/ {
    alias /www/wwwroot/realtimevideo.jgjl.cn/report/;
    try_files $uri $uri/ /report/index.html;
}

# ──── 报告系统后端 API ────
location /report/api/ {
    rewrite ^/report(/.*)$ $1 break;
    proxy_pass http://127.0.0.1:8082;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_connect_timeout 10s;
    proxy_read_timeout 60s;
    proxy_send_timeout 60s;
}

location /report/actuator/ {
    rewrite ^/report(/.*)$ $1 break;
    proxy_pass http://127.0.0.1:8082;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}

location /report/admin/ {
    rewrite ^/report(/.*)$ $1 break;
    proxy_pass http://127.0.0.1:8082;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}

location /report/assets/ {
    alias /www/wwwroot/realtimevideo.jgjl.cn/report/assets/;
    expires 1y;
    add_header Cache-Control "public, immutable";
}
```

保存后 Nginx 自动重载。

---

### 第 5 步：验证

浏览器打开 **`https://realtimevideo.jgjl.cn/report/`**

默认账号：`admin` / `.env` 中设置的密码

---

## 以后更新代码

```bash
# 本地重新编译
mvn clean package -DskipTests
cd frontend && npm run build && cd ..

# 更新部署包
cp target/report-springboot-1.0.0.jar report-deploy/
rm -rf report-deploy/frontend
cp -r frontend/dist report-deploy/frontend
# 重新打包 report-deploy 目录为 tar.gz，上传到服务器

# 服务器上
cd /opt/report
tar -xzf 新包.tar.gz -C .
docker compose up -d --build
```
