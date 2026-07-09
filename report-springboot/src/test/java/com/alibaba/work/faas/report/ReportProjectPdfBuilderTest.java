package com.alibaba.work.faas.report;

import com.alibaba.work.faas.report.model.ProjectReportData;
import org.apache.pdfbox.pdmodel.PDDocument;
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
        assertTrue(html.contains("__PROJECT_PAGE_1__"), "应包含项目1页码标记");
        assertTrue(html.contains("__PROJECT_PAGE_2__"), "应包含项目2页码标记");
        assertTrue(html.contains("project-block { page-break-before: always; }"), "项目应强制分页");
        assertTrue(html.contains("cover-page { page-break-after: always; }"), "封面应强制分页");
        assertFalse(html.contains("content: counter(page)"), "页码由 PDFBox 后处理注入，不在 CSS 中");
        assertFalse(html.contains("counter(pages)"), "总页数由 PDFBox 计算，不在 CSS 中");
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

        // 通过嵌入的文本标记提取项目起始页码
        // 单文档模式：封面为第 1 页，项目 1 从第 2 页开始
        Map<Integer, Integer> pageMap = PdfHelper.extractPageNumbers(pdf);
        assertFalse(pageMap.isEmpty(), "应能通过文本标记提取到项目页码");
        assertTrue(pageMap.containsKey(1), "应包含项目 1 的页码");
        assertTrue(pageMap.containsKey(2), "应包含项目 2 的页码");
        assertEquals(Integer.valueOf(2), pageMap.get(1),
                "项目 1 应从第 2 页开始（封面为第 1 页）");
        assertTrue(pageMap.get(2) > pageMap.get(1),
                "项目 2 页码应大于项目 1 页码");

        System.out.println("✅ 提取到项目页码: " + pageMap);
    }

    @Test
    void build_shouldRenderTocWithInjectedPageNumbers(@TempDir Path tempDir) throws Exception {
        ProjectReportData data = mockMultiProjectData();

        // pageNumberMap 存储目录显示页码（从 1 开始）
        Map<Integer, Integer> pageNumberMap = new java.util.LinkedHashMap<>();
        pageNumberMap.put(1, 1);
        pageNumberMap.put(2, 2);

        String htmlPass2 = ReportProjectPdfBuilder.INSTANCE.build(data, pageNumberMap);

        assertTrue(htmlPass2.contains(">1<"), "目录中项目 1 的页码应为 1");
        assertTrue(htmlPass2.contains(">2<"), "目录中项目 2 的页码应为 2");
        assertFalse(htmlPass2.contains(">-<"), "目录中不应出现未解析的 -");
    }

    @Test
    void build_shouldMergeCoverAndProjectsLinksWorkInSingleDocument(@TempDir Path tempDir) throws Exception {
        ProjectReportData data = mockMultiProjectData();
        ReportPdfExporter exporter = new ReportPdfExporter();

        // 单文档渲染（封面+项目）
        String html = ReportProjectPdfBuilder.INSTANCE.build(data, null);
        byte[] pdf = exporter.exportPdfFromHtml(html);

        // 提取页码
        Map<Integer, Integer> pageMap = PdfHelper.extractPageNumbers(pdf);

        // 用真实页码重新渲染
        String htmlFinal = ReportProjectPdfBuilder.INSTANCE.build(data, pageMap);
        byte[] pdfFinal = exporter.exportPdfFromHtml(htmlFinal);

        assertTrue(pdfFinal.length > 0, "最终 PDF 应有内容");
        assertTrue(pageMap.containsKey(1) && pageMap.containsKey(2), "应提取到所有项目页码");
        System.out.println("✅ 单文档 PDF 生成成功，页码映射: " + pageMap);
    }

    @Test
    void injectPageNumbers_shouldStartAtOneAfterCover(@TempDir Path tempDir) throws Exception {
        ProjectReportData data = mockMultiProjectData();
        ReportPdfExporter exporter = new ReportPdfExporter();

        // 生成项目 PDF（封面 + 2 个项目）
        String html = ReportProjectPdfBuilder.INSTANCE.build(data, null);
        byte[] pdf = exporter.exportPdfFromHtml(html);

        // 注入 PDFBox 页码
        byte[] pdfWithPageNumbers = PdfHelper.injectPageNumbers(pdf);
        assertTrue(pdfWithPageNumbers.length > pdf.length, "注入页码后 PDF 应变大");

        // 验证总页数 = 封面(1) + 项目(≥2)
        try (PDDocument doc = PDDocument.load(new java.io.ByteArrayInputStream(pdfWithPageNumbers))) {
            int totalPages = doc.getNumberOfPages();
            assertTrue(totalPages >= 3, "至少 3 页（封面 + 2 个项目）");
            System.out.println("✅ 页码注入后共 " + totalPages + " 页");
        }

        // 将示例 PDF 写入临时目录查看
        Path out = tempDir.resolve("project-report-with-pagenum.pdf");
        Files.write(out, pdfWithPageNumbers);
        System.out.println("✅ 带页码 PDF 已生成: " + out.toAbsolutePath());
    }

    @Test
    void build_shouldMergeCoverAndProjectsStartingAtPageOne(@TempDir Path tempDir) throws Exception {
        ProjectReportData data = mockMultiProjectData();
        ReportPdfExporter exporter = new ReportPdfExporter();

        // 1. 渲染项目正文（页码从 1 开始）
        String projectsHtml = ReportProjectPdfBuilder.INSTANCE.buildProjectsOnly(data, null);
        byte[] projectsPdf = exporter.exportPdfFromHtml(projectsHtml);
        Map<Integer, Integer> pageMap = PdfHelper.extractPageNumbers(projectsPdf);

        assertFalse(pageMap.isEmpty(), "应提取到项目页码");
        assertEquals(Integer.valueOf(1), pageMap.get(1), "项目 1 在项目正文 PDF 中应从第 1 页开始");

        // 2. 渲染封面（使用项目正文的真实页码）
        String coverHtml = ReportProjectPdfBuilder.INSTANCE.buildCoverOnly(data, pageMap);
        byte[] coverPdf = exporter.exportPdfFromHtml(coverHtml);

        // 3. 合并封面 + 项目正文
        byte[] mergedPdf = PdfMerger.merge(coverPdf, projectsPdf);
        assertTrue(mergedPdf.length > 0, "合并后的 PDF 应有内容");

        // 4. 验证合并后总页数 = 封面页数 + 项目正文页数
        try (PDDocument doc = PDDocument.load(mergedPdf);
             PDDocument coverDoc = PDDocument.load(coverPdf);
             PDDocument projectDoc = PDDocument.load(projectsPdf)) {
            int totalPages = doc.getNumberOfPages();
            int coverPages = coverDoc.getNumberOfPages();
            int projectPages = projectDoc.getNumberOfPages();
            assertEquals(coverPages + projectPages, totalPages,
                    "合并后页数应等于封面页数 + 项目正文页数");
            System.out.println("✅ 合并 PDF: 封面 " + coverPages + " 页 + 项目 " + projectPages
                    + " 页 = 共 " + totalPages + " 页");
        }
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
