# 项目报告 Builder xm.js 风格重构完成

## 改动概述

将项目报告的 HTML/PDF 展示层从平台报告风格（表格布局）完全重构为 `xm.js` 风格（每个项目独立完整区块）。

## 改动的文件

### 1. `ProjectReportStrategy.java` — v2 数据模型适配

- `executeSingleProject()`：构建单个 `PerProjectReport` 包装为列表
- `executeAllProjects()`：先批查全部数据，然后按项目名称过滤（`buildSections(projectName, allSourceData)`），为每个项目生成独立的 `PerProjectReport`
- 删除了旧的平面 `buildSections()` 方法，新增带项目过滤的重载

### 2. `ReportProjectHtmlBuilder.java` — 完全重写

**全项目模式下输出结构（浏览器版）：**
```
┌─ 头部（标题、统计范围、项目数）─────────────┐
│                                              │
├─ 项目 1 区块 ────────────────────────────────┤
│   ├─ [1] 项目名称                            │
│   ├─ 项目信息卡片（报告时间/总监/专监/地址）  │
│   ├─ 统计卡片（6个数据源计数）               │
│   ├─ 📚 资料库（可折叠 ▸ 按分类分组）        │
│   ├─ 📊 项目动态（可折叠 ▸ 提交人/标题/日期）│
│   ├─ 📋 监理日志（可折叠 ▸ 提交人/日期）     │
│   ├─ 🛡️ 日志(安全)（可折叠 ▸ 提交人/日期）   │
│   ├─ 🔍 旁站记录（可折叠 ▸ 提交人/日期）     │
│   ├─ ⚠️ 安全隐患台账（可折叠 ▸ 标签状态）    │
├─ 项目 2 区块 ────────────────────────────────┤
│   ...（同上）                                │
└──────────────────────────────────────────────┘
```

- 使用 `<details><summary>` 实现可折叠（默认展开）
- 安全隐患等级和状态使用彩色标签（同 xm.js）
- 资料库按分类（cascadeSelectField）分组显示
- 项目人员信息显示为紫色标签（总监/专监）

### 3. `ReportProjectPdfBuilder.java` — 同步重写

- 与 HTML Builder 相同结构，使用 table 布局兼容 openhtmltopdf
- 使用 `page-break-inside:avoid` 避免跨项目/跨章节分页
- CSS 2.1 兼容（无 flex/grid/gradient）

## 编译结果

✅ **0 错误，全部编译通过**

## 未改动文件

- `ProjectReportData.java` — 数据模型（v2 已有 `PerProjectReport`/`ProjectBrief`/`SourceSection`）
- `ReportPdfExporter.java` — PDF 导出服务（已正确委托给新的 PDF Builder）
- 平台报告相关文件（`ReportHtmlBuilder.java`、`ReportPdfBuilder.java`、`ReportData.java`）
