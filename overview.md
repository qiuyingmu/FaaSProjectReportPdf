# 周期报告合并功能已完成 ✅

**develop 分支：** https://github.com/qiuyingmu/FaaSProjectReportPdf/tree/develop

## 变更说明

### 1. 报告类型变更
- 不再区分"平台报告"和"全项目报告"类型
- 改为按周期区分：**周报**、**月报**、**季报**（后续可扩展半年报/年报）
- 新增 `ReportPeriod.java` 枚举

### 2. 合并报告生成
每次任务执行只生成 **1 份 PDF**，包含：
- **平台报告部分**（前段，无页码）
- 分页符
- **全项目报告部分**（后段，有页码 1/N）
- 两部分通过 PDFBox 合并

### 3. 页码
- 全项目报告页码格式：`1/N`（当前页/总页数）
- 显示在页面底部中间
- **封面页无页脚页码**
- 平台报告部分无页码
- **目录中的“页码”列现在会显示正确的项目起始页码（不再显示“-”）**

### 4. 报告命名格式
- PDF 文件名：`运营报告-周报-6月第1周-(2026-06-01 ~ 2026-06-07)-xxxx.pdf`
- 宜搭表单名称：`运营报告-周报-6月第1周-(2026-06-01 ~ 2026-06-07)`（不含后缀）
- 宜搭 `radioField_mr8y19k0`（运营报告类型）：`周报` / `月报` / `季报`

### 5. 主要改动文件
| 文件 | 改动类型 | 说明 |
|------|---------|------|
| `ReportPeriod.java` | **新增** | 周期枚举 |
| `PdfMerger.java` | **新增** | PDFBox 合并工具 |
| `PdfHelper.java` | **新增/重写** | 从 PDF 提取命名目的地页码；结构推算 fallback |
| `DynamicScheduler.java` | **修改** | 合并报告生成流程 |
| `ReportProjectPdfBuilder.java` | **修改** | 封面/项目分页、页码 CSS、first-project 页码重置 |
| `ReportService.java` | **修改** | 两趟渲染 + 结构推算页码 fallback |
| `YidaFormUpdater.java` | **修改** | 适配新命名 |

## 部署
`report-deploy.tar.gz`（90MB）已重建，包含新后端 JAR。
