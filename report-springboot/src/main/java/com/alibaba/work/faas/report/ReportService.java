package com.alibaba.work.faas.report;

import com.alibaba.work.faas.report.async.YidaFormUpdater;
import com.alibaba.work.faas.report.model.ProjectReportData;
import com.alibaba.work.faas.report.model.ReportRequest;
import com.alibaba.work.faas.report.model.ReportResult;
import com.alibaba.work.faas.report.model.ReportType;
import com.alibaba.work.faas.report.model.TimeRange;
import com.alibaba.work.faas.report.strategy.PlatformReportStrategy;
import com.alibaba.work.faas.report.strategy.ProjectReportStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

/**
 * 报表服务门面 —— 对外统一入口。
 *
 * @author Senior Developer
 * 创建于 2026/07/07
 */
@Service
public class ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportService.class);

    private final ReportStrategyFactory factory;
    private final PlatformReportStrategy platformStrategy;
    private final ProjectReportStrategy projectStrategy;
    private final YidaFormUpdater formUpdater;
    private final ReportPdfExporter pdfExporter;

    public ReportService(ReportStrategyFactory factory,
                          PlatformReportStrategy platformStrategy,
                          ProjectReportStrategy projectStrategy,
                          YidaFormUpdater formUpdater,
                          ReportPdfExporter pdfExporter) {
        this.factory = factory;
        this.platformStrategy = platformStrategy;
        this.projectStrategy = projectStrategy;
        this.formUpdater = formUpdater;
        this.pdfExporter = pdfExporter;
    }


    public List<ReportResult> generate(ReportRequest request) throws Exception {
        return factory.execute(request);
    }


    // ========================================
    //  合并周期报告（周报/月报/季报）
    //  生成一份 PDF：平台报告（无页码）+ 全项目报告（有页码 1/N）
    //  两趟渲染：第一趟获取各项目起始页码，第二趟注入正确页码
    // ========================================

    public Map<String, Object> generateCombinedPeriodReport(TimeRange tr,
                                                              String periodLabel,
                                                              String rangeLabel,
                                                              String dateDisplay) throws Exception {
        String shortCode = UUID.randomUUID().toString().replace("-", "").substring(0, 4);
        String reportBaseName = "运营报告-" + periodLabel + "-" + rangeLabel
                + "-(" + dateDisplay + ")";

        log.info("▶ 生成合并报告: {}", reportBaseName);

        // 1. 生成平台报告 PDF
        ReportRequest platRequest = new ReportRequest(
                ReportType.PLATFORM, Collections.singletonList(tr), null, null);
        List<ReportResult> platResults = generate(platRequest);
        byte[] platformPdf = (platResults != null && !platResults.isEmpty())
                ? platResults.get(0).getPdfBytes() : null;

        // 2. 生成全项目报告（两趟渲染获取页码）
        ReportRequest projRequest = new ReportRequest(
                ReportType.PROJECT, Collections.singletonList(tr), null, null);

        byte[] projectPdf = null;
        Map<String, Object> projDataMap = projectStrategy.buildProjectReportData(projRequest);
        if (projDataMap != null && Boolean.TRUE.equals(projDataMap.get("isMultiProject"))) {
            @SuppressWarnings("unchecked")
            List<ProjectReportData> dataList = (List<ProjectReportData>) projDataMap.get("dataList");
            if (dataList != null && !dataList.isEmpty()) {
                projectPdf = renderProjectPdfWithPageNumbers(dataList.get(0));
            }
        }
        // 退路：如果策略未返回数据，用标准 generate 流程
        if (projectPdf == null) {
            List<ReportResult> fallbackResults = generate(projRequest);
            if (fallbackResults != null && !fallbackResults.isEmpty()) {
                projectPdf = fallbackResults.get(0).getPdfBytes();
            }
        }

        if (platformPdf == null && projectPdf == null) {
            log.warn("⚠ 平台报告和项目报告均无数据，跳过");
            return null;
        }

        // 3. 合并 PDF
        byte[] combinedPdf = PdfMerger.merge(
                platformPdf != null ? platformPdf : new byte[0],
                projectPdf != null ? projectPdf : new byte[0]);

        // 4. 创建宜搭记录
        String formInstId = formUpdater.createReportRecord(
                reportBaseName, periodLabel, rangeLabel, dateDisplay);

        if (formInstId == null || formInstId.isEmpty()) {
            log.error("创建宜搭记录失败，跳过 {}", reportBaseName);
            return null;
        }

        // 5. 上传到 OBS
        Map<String, String> obsInfo = formUpdater.uploadToObs(
                combinedPdf, periodLabel + "报告", reportBaseName);

        // 6. 更新宜搭记录
        formUpdater.updateReportRecord(formInstId,
                Collections.singletonList(obsInfo), dateDisplay, true, "已完成");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("reportName", reportBaseName);
        result.put("formInstId", formInstId);
        result.put("obsUrl", obsInfo.get("previewUrl"));
        result.put("pdfSize", combinedPdf.length);
        result.put("shortCode", shortCode);
        log.info("✅ 合并报告完成: {} ({} KB, formId={})",
                reportBaseName, combinedPdf.length / 1024, formInstId);
        return result;
    }

    /**
     * 生成项目报告 PDF，并在目录中注入各项目起始页码。
     *
     * <p>页码通过两阶段确定：优先从第一趟渲染的 PDF 中提取嵌入的文本标记；
     * 若提取失败，则按文档结构推算（封面固定 1 页 + 每个项目强制从新页开始）。</p>
     */
    private byte[] renderProjectPdfWithPageNumbers(ProjectReportData projectData) throws Exception {
        long start = System.currentTimeMillis();

        // 第一趟：渲染无页码版本，用于提取真实页码
        String xhtmlPass1 = ReportProjectPdfBuilder.INSTANCE.build(projectData, null);
        byte[] pdfPass1 = exportPdfFromHtml(xhtmlPass1);

        // 提取项目页码映射（project index → page number）
        Map<Integer, Integer> pageNumberMap = extractPageNumberMap(projectData, pdfPass1);

        if (pageNumberMap.isEmpty()) {
            log.warn("[ReportService] 未提取到项目页码，按文档结构推算");
            pageNumberMap = calculateStructuralPageNumbers(projectData);
        }

        if (pageNumberMap.isEmpty()) {
            log.warn("[ReportService] 无项目页码可用，使用无页码版本");
            return pdfPass1;
        }

        // 第二趟：渲染带正确页码的版本
        String xhtmlPass2 = ReportProjectPdfBuilder.INSTANCE.build(projectData, pageNumberMap);
        byte[] pdfPass2 = exportPdfFromHtml(xhtmlPass2);

        log.info("[ReportService] 项目报告渲染完成，共 {} 个项目页码，总耗时 {}ms",
                pageNumberMap.size(), System.currentTimeMillis() - start);
        return pdfPass2;
    }

    private Map<Integer, Integer> extractPageNumberMap(ProjectReportData projectData, byte[] pdfBytes)
            throws IOException {
        Map<Integer, Integer> pageMap = PdfHelper.extractPageNumbers(pdfBytes);
        Map<Integer, Integer> pageNumberMap = new LinkedHashMap<>();

        int projectCount = projectData.getProjectReports().size();
        for (Map.Entry<Integer, Integer> e : pageMap.entrySet()) {
            int index = e.getKey();
            if (index >= 1 && index <= projectCount) {
                pageNumberMap.put(index, e.getValue());
            }
        }

        if (!pageNumberMap.isEmpty()) {
            log.info("[ReportService] 从 PDF 提取到 {} 个项目页码", pageNumberMap.size());
        }

        return pageNumberMap;
    }

    /**
     * 按文档结构推算项目起始页码（fallback）。
     *
     * <p>封面页通过命名页面规则与项目部分分离并隐藏页脚，项目部分通过
     * {@code @page project} 从 1 开始编号。每个项目强制从新页开始，
     * 因此项目 i 的起始页码为 i。</p>
     */
    private Map<Integer, Integer> calculateStructuralPageNumbers(ProjectReportData projectData) {
        Map<Integer, Integer> pageNumberMap = new LinkedHashMap<>();
        int projectCount = projectData.getProjectReports().size();
        for (int i = 1; i <= projectCount; i++) {
            pageNumberMap.put(i, i);
        }
        log.info("[ReportService] 按结构推算 {} 个项目页码", projectCount);
        return pageNumberMap;
    }

    private byte[] exportPdfFromHtml(String xhtml) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(65536);
        pdfExporter.renderToStream(xhtml, baos);
        return baos.toByteArray();
    }


    // ========================================
    //  向后兼容方法
    // ========================================

    @Deprecated
    public ReportData loadStats(String range) throws Exception {
        return platformStrategy.loadStats(range);
    }

    public List<ReportResult> generateFromInput(Map<String, Object> input) throws Exception {
        ReportRequest request = ReportRequest.fromInput(input);
        return generate(request);
    }

    public List<ReportResult> generatePlatformReport(TimeRange... timeRanges) throws Exception {
        ReportRequest request = new ReportRequest(
                ReportType.PLATFORM,
                Arrays.asList(timeRanges),
                null, null);
        return generate(request);
    }
}
