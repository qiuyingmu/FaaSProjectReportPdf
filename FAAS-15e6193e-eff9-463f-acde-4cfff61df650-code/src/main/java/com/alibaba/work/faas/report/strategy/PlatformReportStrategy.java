package com.alibaba.work.faas.report.strategy;

import com.alibaba.work.faas.report.*;
import com.alibaba.work.faas.report.model.ReportRequest;
import com.alibaba.work.faas.report.model.ReportResult;
import com.alibaba.work.faas.report.model.TimeRange;
import com.alibaba.work.faas.service.YidaApiManager;
import com.aliyun.dingtalkyida_2_0.models.SearchFormDatasResponseBody;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

/**
 * 平台报告策略 —— 概览所有项目的统计数据。
 *
 * <p>从 {@link ReportService} 中提取核心查询逻辑，封装为策略实现。
 * 采用「按数据源批量查询 → 内存聚合」性能优化策略。</p>
 *
 * <h3>性能说明</h3>
 * <pre>
 *   优化前：1（项目列表）+ 项目数 × 6（数据源）次 API 调用
 *   优化后：1（项目列表）+  6（数据源）次 API 调用
 *   效果：从 109 次 API 降至 7 次，与项目数无关
 * </pre>
 *
 * @author Senior Developer
 * 创建于 2026/07/07
 */
public class PlatformReportStrategy implements ReportStrategy {

    // ========================================
    //  饿汉式单例
    // ========================================

    public static final PlatformReportStrategy INSTANCE = new PlatformReportStrategy();

    private final YidaApiManager api;

    private PlatformReportStrategy() {
        this.api = YidaApiManager.INSTANCE;
    }


    // ========================================
    //  策略接口实现
    // ========================================

    @Override
    public List<ReportResult> execute(ReportRequest request) throws Exception {
        long totalStart = System.currentTimeMillis();
        System.out.println("[PlatformReportStrategy] 开始执行，时间范围: " + request.getTimeRanges());

        List<ReportResult> results = new ArrayList<>();

        for (TimeRange tr : request.getTimeRanges()) {
            String range = tr.toReportRange();
            long start = System.currentTimeMillis();

            // 1. 查询数据
            ReportData data = loadStats(range);

            // 2. 生成 HTML（浏览器版）
            String html = ReportHtmlBuilder.INSTANCE.build(data);

            // 3. 导出 PDF
            byte[] pdfBytes = ReportPdfExporter.INSTANCE.exportPdf(data);

            long cost = System.currentTimeMillis() - start;
            System.out.println("  [" + tr.getLabel() + "] 完成，"
                    + data.getProjects().size() + " 个项目，"
                    + data.getTotalRecords() + " 条记录，"
                    + "PDF " + (pdfBytes.length / 1024) + " KB，耗时 " + cost + "ms");

            results.add(new ReportResult(tr, html, pdfBytes,
                    data.getTotalRecords(), data.getTimeRangeLabel()));
        }

        long totalCost = System.currentTimeMillis() - totalStart;
        System.out.println("[PlatformReportStrategy] 全部完成，共 " + results.size()
                + " 份报告，总耗时 " + totalCost + "ms");

        return results;
    }


    // ========================================
    //  核心统计方法（从 ReportService 提取）
    // ========================================

    /**
     * 加载平台报告数据 —— 所有项目的统计概览。
     *
     * @param range 时间范围：week / lastWeek / month / lastMonth
     * @return 报表数据
     */
    public ReportData loadStats(String range) throws Exception {
        ReportDateUtils.DateRange dr = ReportDateUtils.getRange(range);
        long tick = System.currentTimeMillis();

        // ---- 1. 查询项目列表 ----
        List<SearchFormDatasResponseBody.SearchFormDatasResponseBodyData> rawProjects =
                api.searchAllFormData(ReportConstants.FORM_PROJECT, null);

        // 构建项目名称 → 项目行 的映射
        Map<String, SearchFormDatasResponseBody.SearchFormDatasResponseBodyData> projectMap = new LinkedHashMap<>();
        if (rawProjects != null) {
            for (SearchFormDatasResponseBody.SearchFormDatasResponseBodyData row : rawProjects) {
                Map<String, ?> fd = row.getFormData();
                if (fd == null) continue;
                String name = ReportHelper.extractField(fd, ReportConstants.F_PROJECT_NAME);
                if (StringUtils.isBlank(name)) continue;
                if (name.contains("测试") || name.toLowerCase().contains("test")) continue;
                projectMap.put(name, row);
            }
        }

        System.out.println("[PlatformReportStrategy] 项目列表加载完成，"
                + projectMap.size() + " 个项目，耗时 "
                + (System.currentTimeMillis() - tick) + "ms");

        // ---- 2. 并行查询 6 个数据源 + 内存聚合 ----
        // 6 个数据源互不依赖，并行执行减少总耗时
        Map<ReportConstants.SourceDef, Map<String, Integer>> parallelResults =
                ReportParallel.parallelMap(
                        ReportConstants.SOURCES,
                        src -> {
                            try {
                                return loadSourceCountsBatch(src, dr.start, dr.end);
                            } catch (Exception e) {
                                throw new RuntimeException("查询数据源[" + src.label + "]失败: " + e.getMessage(), e);
                            }
                        },
                        "平台报告-6数据源查询"
                );

        // 按 key 整理结果
        Map<String, Map<String, Integer>> sourceMatrix = new LinkedHashMap<>();
        for (Map.Entry<ReportConstants.SourceDef, Map<String, Integer>> entry : parallelResults.entrySet()) {
            String key = entry.getKey().key;
            Map<String, Integer> counts = entry.getValue();
            sourceMatrix.put(key, counts);
            int total = counts.values().stream().mapToInt(Integer::intValue).sum();
            System.out.println("[PlatformReportStrategy] 数据源 [" + entry.getKey().label
                    + "] 完成，共 " + total + " 条，涉及 " + counts.size() + " 个项目");
        }

        // ---- 3. 组装每个项目的统计数据 ----
        List<ReportData.ProjectStat> projects = new ArrayList<>();
        int grandTotal = 0;

        for (Map.Entry<String, SearchFormDatasResponseBody.SearchFormDatasResponseBodyData> entry : projectMap.entrySet()) {
            String name = entry.getKey();
            SearchFormDatasResponseBody.SearchFormDatasResponseBodyData row = entry.getValue();
            Map<String, ?> fd = row.getFormData();

            String director = ReportHelper.extractField(fd, ReportConstants.F_PROJECT_DIRECTOR);
            String addr = ReportHelper.extractField(fd, ReportConstants.F_PROJECT_ADDR);
            String area = ReportHelper.extractDistrict(addr);

            Map<String, Integer> sourceCounts = new LinkedHashMap<>();
            int projectTotal = 0;

            for (ReportConstants.SourceDef src : ReportConstants.SOURCES) {
                Map<String, Integer> projectCounts = sourceMatrix.get(src.key);
                int count = projectCounts != null ? projectCounts.getOrDefault(name, 0) : 0;
                sourceCounts.put(src.key, count);
                projectTotal += count;
            }

            projects.add(new ReportData.ProjectStat(
                    row.getFormInstanceId(), name, director, area,
                    sourceCounts, projectTotal));
            grandTotal += projectTotal;
        }

        long total = System.currentTimeMillis() - tick;
        System.out.println("[PlatformReportStrategy] 报表生成完成，"
                + projects.size() + " 个项目，总计 " + grandTotal + " 条记录，总耗时 " + total + "ms");

        return new ReportData(
                ReportDateUtils.rangeLabel(range),
                ReportDateUtils.periodName(range),
                range,
                projects,
                grandTotal
        );
    }


    // ========================================
    //  批量查询
    // ========================================

    /**
     * 批量查询单个数据源在某时间范围内的记录，按项目名称内存聚合。
     */
    private Map<String, Integer> loadSourceCountsBatch(ReportConstants.SourceDef src,
                                                        Date start, Date end) throws Exception {
        String searchFieldJson = ReportHelper.buildDateOnlyFilter(src.dateField, start, end);

        List<SearchFormDatasResponseBody.SearchFormDatasResponseBodyData> allRecords =
                api.searchAllFormData(src.formUuid, searchFieldJson);

        Map<String, Integer> counts = new HashMap<>();
        if (allRecords != null) {
            for (SearchFormDatasResponseBody.SearchFormDatasResponseBodyData row : allRecords) {
                String projectName = ReportHelper.extractField(row.getFormData(), src.personField);
                if (projectName != null && !projectName.isEmpty()) {
                    counts.merge(projectName, 1, Integer::sum);
                }
            }
        }
        return counts;
    }
}
