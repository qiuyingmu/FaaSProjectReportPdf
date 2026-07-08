# Spring Boot 运营报告定时任务项目

## 项目结构

```
report-springboot/
├── pom.xml                                # Spring Boot 2.6.6 + 所有依赖
├── .gitignore
└── src/main/
    ├── java/com/alibaba/work/faas/
    │   ├── ReportApplication.java         [NEW]  @EnableScheduling 入口
    │   ├── config/
    │   │   └── ReportConfig.java          [NEW]  调度线程池配置
    │   ├── schedule/
    │   │   └── ReportScheduler.java       [NEW]  定时任务（周/月/季度）
    │   ├── report/                        [COPY] 全部业务逻辑（24个文件）
    │   │   ├── model/     (5 files)       数据模型
    │   │   ├── strategy/  (3 files)       报告策略
    │   │   ├── async/     (3 files)       OBS上传、表单更新
    │   │   └── *.java     (12 files)      构建器、常量、工具类
    │   └── service/
    │       └── YidaApiManager.java        [COPY] 钉钉Token管理
    └── resources/
        ├── application.yml                [NEW]  Spring配置
        ├── yida-secret.properties         [COPY] 敏感凭证
        └── fonts/msyh.ttc                 [COPY] 中文字体
```

## 与原 FaaS 项目的区别

| 维度 | FaaS 项目 | 新 Spring Boot 项目 |
|------|----------|-------------------|
| **入口** | `FaasEntry.execute()` | `ReportApplication.main()` |
| **触发方式** | 宜搭集成自动化 → FaaS | 内建定时任务 |
| **异步处理** | 手动线程池（可能被FaaS中断） | Spring `@Scheduled`（可靠） |
| **报告类型** | 单次调用只生成一种 | 每次生成两种（平台+项目） |
| **部署** | 宜搭 FaaS 容器 | 独立 JVM 进程 |

## 定时任务

- **周报**：每周一 08:00 → `lastWeek`
- **月报**：每月 1 日 08:00 → `lastMonth`
- **季报**：1/4/7/10 月 1 日 08:00 → `lastQuarter`

## 执行流程（每个任务）

1. 获取 accessToken
2. 生成 **平台报告**（平台报告策略 + 上月范围）
3. 上传 PDF 到华为 OBS
4. 创建并更新宜搭运营报告表单
5. 生成 **项目报告**（全项目汇总 + 上月范围）
6. 上传 PDF 到华为 OBS
7. 创建并更新宜搭运营报告表单

## 编译结果

✅ **37 个 class，0 错误**

## 运行方式

```bash
cd report-springboot
mvn spring-boot:run
# 或打包后运行
mvn package -DskipTests
java -jar target/report-springboot-1.0.0.jar
```
