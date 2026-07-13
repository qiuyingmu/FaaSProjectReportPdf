package com.alibaba.work.faas.report.strategy;

import com.alibaba.work.faas.report.*;
import com.alibaba.work.faas.report.model.ReportRequest;
import com.alibaba.work.faas.report.model.ReportResult;
import com.alibaba.work.faas.report.model.TimeRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 平台报告策略 —— 概览所有项目的统计数据。
 *
 * @author Senior Developer
 * 创建于 2026/07/07
 */
@Service
public class PlatformReportStrategy implements ReportStrategy {

    private static final Logger log = LoggerFactory.getLogger(PlatformReportStrategy.class);

    private final ReportQueryService queryService;
    private final ReportPdfExporter pdfExporter;

    public PlatformReportStrategy(ReportQueryService queryService, ReportPdfExporter pdfExporter) {
        this.queryService = queryService;
        this.pdfExporter = pdfExporter;
    }


    // ========================================
    //  策略接口实现
    // ========================================

    @Override
    public List<ReportResult> execute(ReportRequest request) throws Exception {
        long totalStart = System.currentTimeMillis();
        log.info("[PlatformReportStrategy] 开始执行，时间范围: " + request.getTimeRanges());

        List<ReportResult> results = new ArrayList<>();

        for (TimeRange tr : request.getTimeRanges()) {
            String range = tr.toReportRange();
            long start = System.currentTimeMillis();

            // 1. 查询数据（优先使用调用方传入的 periodLabel，消除 rangeToPeriodLabel 推断歧义）
            String pLabel = request.getPeriodLabel();
            ReportData data = (pLabel != null) ? loadStats(range, pLabel) : loadStats(range);

            // 2. 生成 HTML（浏览器版）
            String html = ReportHtmlBuilder.INSTANCE.build(data);

            // 3. 导出 PDF
            byte[] pdfBytes = pdfExporter.exportPdf(data);

            long cost = System.currentTimeMillis() - start;
            log.info("  [" + tr.getLabel() + "] 完成，"
                    + data.getProjects().size() + " 个项目，"
                    + data.getTotalRecords() + " 条记录，"
                    + "PDF " + (pdfBytes.length / 1024) + " KB，耗时 " + cost + "ms");

            results.add(new ReportResult(tr, html, pdfBytes,
                    data.getTotalRecords(), data.getTimeRangeLabel()));
        }

        long totalCost = System.currentTimeMillis() - totalStart;
        log.info("[PlatformReportStrategy] 全部完成，共 " + results.size()
                + " 份报告，总耗时 " + totalCost + "ms");

        return results;
    }


    // ========================================
    //  核心统计方法（从 ReportService 提取）
    // ========================================

    /**
     * 加载平台报告数据 —— 所有项目的统计概览。
     *
     * @param range 时间范围代码（lastWeek/lastMonth/lastQuarter 等）
     * @param periodLabel 周期标签（周报/月报/季报），null 时自动从 range 推断
     */
    public ReportData loadStats(String range, String periodLabel) throws Exception {
        ReportDateUtils.DateRange dr = ReportDateUtils.getRange(range);
        long tick = System.currentTimeMillis();

        // 1. 查询所有项目
        List<ReportQueryService.ProjectInfo> allProjects = queryService.resolveAllProjects();
        log.info("[PlatformReportStrategy] 项目列表加载完成，{} 个项目，耗时 {}ms",
                allProjects.size(), System.currentTimeMillis() - tick);

        // 2. 并行查询 6 个数据源并按项目聚合
        Map<String, Map<String, Integer>> sourceMatrix = queryService.loadSourceCounts(dr.start, dr.end);

        // 3. 组装每个项目的统计数据
        List<ReportData.ProjectStat> projects = new ArrayList<>();
        int grandTotal = 0;

        for (ReportQueryService.ProjectInfo pi : allProjects) {
            Map<String, Integer> sourceCounts = new LinkedHashMap<>();
            int projectTotal = 0;
            for (ReportConstants.SourceDef src : ReportConstants.SOURCES) {
                Map<String, Integer> projectCounts = sourceMatrix.get(src.key);
                int count = projectCounts != null ? projectCounts.getOrDefault(pi.name, 0) : 0;
                sourceCounts.put(src.key, count);
                projectTotal += count;
            }
            projects.add(new ReportData.ProjectStat(
                    pi.instId, pi.name, pi.director, pi.area,
                    sourceCounts, projectTotal));
            grandTotal += projectTotal;
        }

        // ...（日志和返回与原来保持一致）
        long total = System.currentTimeMillis() - tick;
        log.info("[PlatformReportStrategy] 报表生成完成，{} 个项目，总计 {} 条记录，总耗时 {}ms",
                projects.size(), grandTotal, total);

        // periodLabel 由调用方传入，消除 rangeToPeriodLabel 推断的不确定性
        String label = (periodLabel != null) ? periodLabel : ReportDateUtils.rangeToPeriodLabel(range);

        return new ReportData(
                ReportDateUtils.formatRangeLabel(label, dr.start, dr.end),
                ReportDateUtils.periodName(range),
                range,
                projects,
                grandTotal
        );
    }

    /**
     * 加载平台报告数据 —— 兼容旧调用（不传 periodLabel）
     */
    public ReportData loadStats(String range) throws Exception {
        return loadStats(range, null);
    }

}
