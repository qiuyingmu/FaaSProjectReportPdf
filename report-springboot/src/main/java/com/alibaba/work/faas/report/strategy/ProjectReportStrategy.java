package com.alibaba.work.faas.report.strategy;

import com.alibaba.work.faas.report.*;
import com.alibaba.work.faas.report.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 项目报告策略（v2 —— 按项目分组的完整报告）。
 *
 * @author Senior Developer
 * 创建于 2026/07/07
 */
@Service
public class ProjectReportStrategy implements ReportStrategy {

    private static final Logger log = LoggerFactory.getLogger(ProjectReportStrategy.class);

    private final ReportQueryService queryService;
    private final ReportPdfExporter pdfExporter;

    public ProjectReportStrategy(ReportQueryService queryService, ReportPdfExporter pdfExporter) {
        this.queryService = queryService;
        this.pdfExporter = pdfExporter;
    }


    // ========================================
    //  策略接口实现
    // ========================================

    @Override
    public List<ReportResult> execute(ReportRequest request) throws Exception {
        long totalStart = System.currentTimeMillis();

        boolean hasProjectParam = StringUtils.isNotBlank(request.getProjectId())
                || StringUtils.isNotBlank(request.getProjectName());

        log.info("[ProjectReportStrategy] 开始执行，模式="
                + (hasProjectParam ? "单项目" : "全项目汇总")
                + ", 时间范围=" + request.getTimeRanges());

        if (hasProjectParam) {
            return executeSingleProject(request);
        } else {
            return executeAllProjects(request);
        }
    }


    // ========================================
    //  单项目模式
    // ========================================

    private List<ReportResult> executeSingleProject(ReportRequest request) throws Exception {
        long totalStart = System.currentTimeMillis();
        String projectId = request.getProjectId();
        String projectName = request.getProjectName();

        ReportQueryService.ProjectInfo info = queryService.resolveProject(projectId, projectName);
        if (info == null) {
            throw new IllegalArgumentException("未找到项目: "
                    + (projectName != null ? projectName : projectId));
        }

        List<ReportResult> results = new ArrayList<>();
        for (TimeRange tr : request.getTimeRanges()) {
            long start = System.currentTimeMillis();
            String range = tr.toReportRange();
            ReportDateUtils.DateRange dr = ReportDateUtils.getRange(range);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            String dateStart = sdf.format(dr.start);
            String dateEnd = sdf.format(dr.end);

            // 查询该项目的全部数据源
            var sourceData = queryService.querySourceDataByProject(info.name, dr.start, dr.end);

            // 构建该项目的数据源段落
            List<ProjectReportData.SourceSection> sections = queryService.buildSections(info.name, sourceData);

            ProjectReportData data = new ProjectReportData(
                    dateStart, dateEnd,
                    ReportDateUtils.formatRangeLabel(ReportDateUtils.rangeToPeriodLabel(range), ReportDateUtils.getRange(range).start, ReportDateUtils.getRange(range).end),
                    ReportDateUtils.periodName(range),
                    Collections.singletonList(new ProjectReportData.PerProjectReport(info.toBrief(), sections)));

            results.add(exportReport(tr, data, start));
        }

        log.info("[ProjectReportStrategy] 单项目完成，共 " + results.size()
                + " 份报告，总耗时 " + (System.currentTimeMillis() - totalStart) + "ms");
        return results;
    }


    // ========================================
    //  全项目汇总模式
    // ========================================

    private List<ReportResult> executeAllProjects(ReportRequest request) throws Exception {
        long totalStart = System.currentTimeMillis();

        List<ReportQueryService.ProjectInfo> allProjectInfo = queryService.resolveAllProjects();
        if (allProjectInfo.isEmpty()) {
            throw new IllegalArgumentException("系统中没有可用的项目");
        }

        List<ReportResult> results = new ArrayList<>();
        for (TimeRange tr : request.getTimeRanges()) {
            long start = System.currentTimeMillis();
            String range = tr.toReportRange();
            ReportDateUtils.DateRange dr = ReportDateUtils.getRange(range);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            String dateStart = sdf.format(dr.start);
            String dateEnd = sdf.format(dr.end);

            // 查询所有数据源的全部数据（按日期范围，无项目过滤）
            var allSourceData = queryService.queryAllSourceData(dr.start, dr.end);

            // 为每个项目构建独立的 PerProjectReport
            List<ProjectReportData.PerProjectReport> projectReports = new ArrayList<>();
            for (ReportQueryService.ProjectInfo pi : allProjectInfo) {
                List<ProjectReportData.SourceSection> sections =
                        queryService.buildSections(pi.name, allSourceData);
                projectReports.add(new ProjectReportData.PerProjectReport(pi.toBrief(), sections));
            }

            ProjectReportData data = new ProjectReportData(
                    dateStart, dateEnd,
                    ReportDateUtils.formatRangeLabel(ReportDateUtils.rangeToPeriodLabel(range), ReportDateUtils.getRange(range).start, ReportDateUtils.getRange(range).end),
                    ReportDateUtils.periodName(range),
                    projectReports);

            results.add(exportReport(tr, data, start));
        }

        log.info("[ProjectReportStrategy] 全项目完成，共 " + results.size()
                + " 份报告，总耗时 " + (System.currentTimeMillis() - totalStart) + "ms");
        return results;
    }


    // ========================================
    //  构建项目报告数据（供 ReportService 两趟渲染用）
    // ========================================

    public Map<String, Object> buildProjectReportData(ReportRequest request) throws Exception {
        Map<String, Object> result = new LinkedHashMap<>();

        boolean hasProjectParam = StringUtils.isNotBlank(request.getProjectId())
                || StringUtils.isNotBlank(request.getProjectName());

        if (hasProjectParam) {
            ReportQueryService.ProjectInfo info = queryService.resolveProject(
                    request.getProjectId(), request.getProjectName());
            if (info == null) throw new IllegalArgumentException("未找到项目");
            result.put("isMultiProject", false);
            return null;
        }

        // 全项目模式
        List<ReportQueryService.ProjectInfo> allProjectInfo = queryService.resolveAllProjects();
        if (allProjectInfo.isEmpty()) throw new IllegalArgumentException("系统中没有可用的项目");

        List<ProjectReportData> dataList = new ArrayList<>();
        for (TimeRange tr : request.getTimeRanges()) {
            String range = tr.toReportRange();
            ReportDateUtils.DateRange dr = ReportDateUtils.getRange(range);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

            var allSourceData = queryService.queryAllSourceData(dr.start, dr.end);

            List<ProjectReportData.PerProjectReport> projectReports = new ArrayList<>();
            for (ReportQueryService.ProjectInfo pi : allProjectInfo) {
                List<ProjectReportData.SourceSection> sections =
                        queryService.buildSections(pi.name, allSourceData);
                projectReports.add(new ProjectReportData.PerProjectReport(pi.toBrief(), sections));
            }

            ProjectReportData data = new ProjectReportData(
                    sdf.format(dr.start), sdf.format(dr.end),
                    ReportDateUtils.formatRangeLabel(ReportDateUtils.rangeToPeriodLabel(range), ReportDateUtils.getRange(range).start, ReportDateUtils.getRange(range).end),
                    ReportDateUtils.periodName(range),
                    projectReports);
            dataList.add(data);
        }

        result.put("isMultiProject", true);
        result.put("dataList", dataList);
        result.put("projectInfos", allProjectInfo);
        return result;
    }


    // ========================================
    //  导出
    // ========================================

    public ReportResult exportData(TimeRange tr, ProjectReportData data) throws Exception {
        return exportReport(tr, data, System.currentTimeMillis());
    }

    private ReportResult exportReport(TimeRange tr, ProjectReportData data, long startTime) throws Exception {
        String html = ReportProjectHtmlBuilder.INSTANCE.build(data);
        byte[] pdfBytes = pdfExporter.exportPdf(data);

        long cost = System.currentTimeMillis() - startTime;
        int totalRec = data.getTotalRecords();
        log.info("  [" + tr.getLabel() + "] 完成，"
                + data.getProjectCount() + " 个项目，"
                + totalRec + " 条记录，"
                + "PDF " + (pdfBytes.length / 1024) + " KB，耗时 " + cost + "ms");

        return new ReportResult(tr, html, pdfBytes,
                totalRec, data.getTimeRangeLabel());
    }
}
