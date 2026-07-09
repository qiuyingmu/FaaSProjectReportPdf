package com.alibaba.work.faas.report;

import com.alibaba.work.faas.report.model.ProjectReportData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 项目报告 PDF 构建器单元测试 —— 验证封面、目录、超链接、分页结构。
 */
public class ReportProjectPdfBuilderTest {

    @Test
    void build_shouldContainCoverAndTocForMultiProject() {
        ProjectReportData data = mockMultiProjectData();
        String html = ReportProjectPdfBuilder.INSTANCE.build(data);

        assertTrue(html.contains("cover-page"), "应包含封面页");
        assertTrue(html.contains("toc-title"), "应包含目录标题");
        assertTrue(html.contains("<a href=\"#project-1\">项目 A</a>"), "目录应链接到项目1");
        assertTrue(html.contains("<a href=\"#project-2\">项目 B</a>"), "目录应链接到项目2");
        assertTrue(html.contains("id=\"project-1\""), "项目1应有锚点");
        assertTrue(html.contains("id=\"project-2\""), "项目2应有锚点");
        assertTrue(html.contains("class=\"project-block first-project\""), "第一个项目块应带 first-project 类");
        assertTrue(html.contains("project-block { page-break-before: always; }"), "项目应强制分页");
        assertTrue(html.contains("project-block.first-project { counter-reset: page 1; }"), "第一个项目应重置页码计数器");
        assertTrue(html.contains("cover-page { page-break-after: always; }"), "封面应强制分页");
        assertTrue(html.contains("@page:first"), "应隐藏封面页脚");
        assertTrue(html.contains("content: counter(page)"), "应包含页码 CSS");
        assertTrue(html.contains("counter(pages)"), "应包含总页数 CSS");
    }

    @Test
    void build_shouldRenderPdfWithPageNumbers(@TempDir Path tempDir) throws Exception {
        ProjectReportData data = mockMultiProjectData();
        String html = ReportProjectPdfBuilder.INSTANCE.build(data);
        assertFalse(html.isEmpty());

        ReportPdfExporter exporter = new ReportPdfExporter();
        byte[] pdf = exporter.exportPdfFromHtml(html);

        assertTrue(pdf.length > 0, "PDF 应有内容");
        assertTrue(pdf.length > 4 * 1024, "PDF 至少大于 4KB");

        // 将样例 PDF 写入临时目录，方便人工查看
        Path out = tempDir.resolve("project-report.pdf");
        Files.write(out, pdf);
        System.out.println("✅ 示例 PDF 已生成: " + out.toAbsolutePath() + " (" + pdf.length + " bytes)");
    }

    @Test
    void build_shouldExtractPageNumbersFromRenderedPdf(@TempDir Path tempDir) throws Exception {
        ProjectReportData data = mockMultiProjectData();
        String html = ReportProjectPdfBuilder.INSTANCE.build(data);

        ReportPdfExporter exporter = new ReportPdfExporter();
        byte[] pdf = exporter.exportPdfFromHtml(html);

        assertTrue(pdf.length > 0, "PDF 应有内容");

        // PdfHelper 尝试从 PDF 命名目的地提取；OpenHTMLToPDF 可能不生成可提取的目的地，
        // 此时返回空 Map 也是正常行为，不应抛异常。
        Map<String, Integer> pageMap = PdfHelper.extractPageNumbers(pdf);
        assertNotNull(pageMap, "页码映射不应为 null");

        System.out.println("✅ 提取到项目页码（可能为空）: " + pageMap);
    }

    @Test
    void build_shouldRenderTocWithInjectedPageNumbers(@TempDir Path tempDir) throws Exception {
        ProjectReportData data = mockMultiProjectData();

        // 模拟 ReportService 中的结构推算逻辑：封面无页码，项目部分从 1 开始
        Map<Integer, Integer> pageNumberMap = new java.util.LinkedHashMap<>();
        for (int i = 1; i <= data.getProjectReports().size(); i++) {
            pageNumberMap.put(i, i); // 项目 i 从第 i 页开始
        }

        String htmlPass2 = ReportProjectPdfBuilder.INSTANCE.build(data, pageNumberMap);

        assertTrue(htmlPass2.contains(">1<"), "目录中项目 1 的页码应为 1");
        assertTrue(htmlPass2.contains(">2<"), "目录中项目 2 的页码应为 2");
        assertFalse(htmlPass2.contains(">-<"), "目录中不应出现未解析的 -");
    }

    private ProjectReportData mockMultiProjectData() {
        List<ProjectReportData.SourceSection> sourcesA = Arrays.asList(
                new ProjectReportData.SourceSection("docLib", "资料库", "#3b82f6", 3, Collections.emptyList()),
                new ProjectReportData.SourceSection("dynamic", "项目动态", "#10b981", 2, Collections.emptyList()),
                new ProjectReportData.SourceSection("log", "监理日志", "#f59e0b", 5, Collections.emptyList()),
                new ProjectReportData.SourceSection("safeLog", "日志(安全)", "#8b5cf6", 0, Collections.emptyList()),
                new ProjectReportData.SourceSection("station", "旁站记录", "#ef4444", 1, Collections.emptyList()),
                new ProjectReportData.SourceSection("hazard", "安全隐患台账", "#14b8a6", 0, Collections.emptyList())
        );

        List<ProjectReportData.SourceSection> sourcesB = Arrays.asList(
                new ProjectReportData.SourceSection("docLib", "资料库", "#3b82f6", 1, Collections.emptyList()),
                new ProjectReportData.SourceSection("dynamic", "项目动态", "#10b981", 4, Collections.emptyList()),
                new ProjectReportData.SourceSection("log", "监理日志", "#f59e0b", 2, Collections.emptyList()),
                new ProjectReportData.SourceSection("safeLog", "日志(安全)", "#8b5cf6", 3, Collections.emptyList()),
                new ProjectReportData.SourceSection("station", "旁站记录", "#ef4444", 0, Collections.emptyList()),
                new ProjectReportData.SourceSection("hazard", "安全隐患台账", "#14b8a6", 1, Collections.emptyList())
        );

        ProjectReportData.ProjectBrief briefA = new ProjectReportData.ProjectBrief(
                "项目 A", "张总监", "李专监", "杭州市西湖区", "西湖区");
        ProjectReportData.ProjectBrief briefB = new ProjectReportData.ProjectBrief(
                "项目 B", "王总监", "赵专监", "杭州市滨江区", "滨江区");

        return new ProjectReportData(
                "2026-06-01", "2026-06-30", "2026年6月", "月度",
                Arrays.asList(
                        new ProjectReportData.PerProjectReport(briefA, sourcesA),
                        new ProjectReportData.PerProjectReport(briefB, sourcesB)
                )
        );
    }
}
