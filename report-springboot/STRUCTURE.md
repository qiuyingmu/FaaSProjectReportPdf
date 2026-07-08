# report-springboot — 重构与项目结构

## 项目结构

```
report-springboot/
├── pom.xml                      # Spring Boot 2.6.6 + Security + Actuator + Thymeleaf
├── .gitignore
├── REFACTOR.md
│
├── frontend/                    # Vue 3 前端（前后端分离）
│   ├── package.json
│   ├── vite.config.js           # 代理 localhost:9001
│   ├── index.html
│   └── src/
│       ├── main.js              # Vue Router + Axios 入口
│       ├── App.vue
│       ├── views/
│       │   ├── Login.vue        # 登录页
│       │   └── Dashboard.vue    # 仪表盘（3 个标签页）
│       └── components/
│           ├── SystemOverview.vue  # 系统概览
│           ├── LogViewer.vue      # 实时日志
│           └── ScheduleManager.vue # 定时任务管理
│
└── src/main/
    ├── java/com/alibaba/work/faas/
    │   ├── ReportApplication.java       @SpringBootApplication + @EnableScheduling
    │   ├── config/
    │   │   ├── ReportConfig.java        ThreadPoolTaskScheduler（3 线程）
    │   │   └── SecurityConfig.java      Spring Security（表单登录）
    │   ├── controller/
    │   │   ├── AdminController.java     管理页面路由
    │   │   ├── LogController.java       日志读取 API
    │   │   └── ScheduleController.java  定时任务 CRUD API
    │   ├── schedule/
    │   │   ├── ScheduleTask.java        任务配置模型
    │   │   └── DynamicScheduler.java    动态调度器
    │   ├── service/
    │   │   └── YidaApiManager.java      @Service 钉钉 Token 管理
    │   └── report/ （全部 @Service 改造完成）
    │       ├── model/       数据模型
    │       ├── strategy/    报告策略（平台/项目）
    │       ├── async/       OBS 上传、表单更新
    │       └── *.java       构建器、常量、工具类
    └── resources/
        ├── application.yml
        ├── logback-spring.xml
        ├── yida-secret.properties
        ├── fonts/msyh.ttc
        └── templates/       Thymeleaf 管理页面（备选）
```

## 运行方式

```bash
# 后端
cd report-springboot
mvn spring-boot:run    # http://localhost:9001/

# 前端（新开终端）
cd report-springboot/frontend
npm install && npm run dev    # http://localhost:3000/
```

默认账号：admin / admin123
