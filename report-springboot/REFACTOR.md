# report-springboot — 重构总结

## 架构变更

### 1. 服务层从饿汉单例 → Spring Bean（构造器注入）

原有 FaaS 适配的 `public static final XXX INSTANCE` 模式已全部注册为 Spring 管理 Bean：

```yaml
config/AppConfig.java            # @Configuration - 将 10 个单例注册为 @Bean
service/YidaApiManager.java      # 钉钉 Token 管理（保留 INSTANCE + AppConfig 注册）
report/ReportService.java        # 报告门面服务
report/ReportStrategyFactory.java # 策略工厂
report/ReportPdfExporter.java    # PDF 导出
report/*Builder.java (4个)       # HTML/PDF 构建器
report/async/YidaFormUpdater.java # 宜搭表单操作
report/async/ReportTaskManager.java # 任务管理
report/strategy/*Strategy.java (2个) # 平台/项目报告策略
```

### 2. 动态调度系统（替代 @Scheduled 注解）

| 文件 | 说明 |
|------|------|
| `schedule/ScheduleTask.java` | 任务配置模型（type/cron/enabled） |
| `schedule/DynamicScheduler.java` | 动态调度器（运行中启/停/改） |
| `controller/ScheduleController.java` | REST API（GET/PUT/toggle） |
| ~~schedule/ReportScheduler.java~~ | 已删除（被 DynamicScheduler 替代） |

### 3. REST API 总览

| 端点 | 方法 | 权限 | 说明 |
|------|------|------|------|
| `/admin/` | GET | ADMIN | 管理后台页面 |
| `/admin/login` | GET | 公开 | 登录页 |
| `/api/admin/logs?lines=N` | GET | ADMIN | 读取最近 N 行日志 |
| `/api/admin/schedules` | GET | ADMIN | 获取所有定时任务配置 |
| `/api/admin/schedules/{type}` | PUT | ADMIN | 更新定时任务（Cron + 启用） |
| `/api/admin/schedules/{type}/toggle` | POST | ADMIN | 启停定时任务 |
| `/actuator/health` | GET | ADMIN | 健康检查 |

### 4. 后台管理页面（admin.html）

三个标签页：
- **📋 实时日志** — 自动刷新，ERROR/WARN 高亮
- **📊 系统概览** — 运行状态、日志大小
- **⏰ 定时任务** — 在线查看/修改 Cron、启停任务

### 5. 安全保护

| 措施 | 说明 |
|------|------|
| Spring Security 表单登录 | 管理页面/API 需认证 |
| 账号可配置 | application.yml `report.admin` |
| CSRF 保护 | 默认启用，API 豁免 |
| 默认安全头 | X-Frame-Options, XSS 保护等 |
| Actuator 受保护 | `/actuator/**` 仅 ADMIN 可访问 |

## 运行方式

```bash
cd report-springboot
mvn spring-boot:run
# 访问 http://localhost:9001/admin/ (admin/admin123)
```
