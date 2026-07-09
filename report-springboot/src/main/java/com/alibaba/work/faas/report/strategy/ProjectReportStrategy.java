package com.alibaba.work.faas.report.strategy;

import com.alibaba.work.faas.report.*;
import com.alibaba.work.faas.report.model.*;
import com.alibaba.work.faas.service.YidaApiManager;
import com.aliyun.dingtalkyida_2_0.models.SearchFormDatasResponseBody;
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

    private final YidaApiManager api;
    private final ReportPdfExporter pdfExporter;

    public ProjectReportStrategy(YidaApiManager api, ReportPdfExporter pdfExporter) {
        this.api = api;
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

        ProjectInfo info = resolveProject(projectId, projectName);
        if (info == null) {
            throw new IllegalArgumentException("未找到项目: "
                    + (projectName != null ? projectName : projectId));
        }
        log.info("[ProjectReportStrategy] 项目信息: "
                + info.name + " / " + info.director + " / " + info.engineer);

        ProjectReportData.ProjectBrief brief = new ProjectReportData.ProjectBrief(
                info.name, info.director, info.engineer, info.address, info.area);

        List<ReportResult> results = new ArrayList<>();
        for (TimeRange tr : request.getTimeRanges()) {
            long start = System.currentTimeMillis();
            String range = tr.toReportRange();
            ReportDateUtils.DateRange dr = ReportDateUtils.getRange(range);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            String dateStart = sdf.format(dr.start);
            String dateEnd = sdf.format(dr.end);

            // 查询该项目的全部数据源
            Map<String, List<SearchFormDatasResponseBody.SearchFormDatasResponseBodyData>> sourceData
                    = querySourceDataByProject(info.name, dr.start, dr.end);

            // 构建该项目的数据源段落
            List<ProjectReportData.SourceSection> sections = buildSections(info.name, sourceData);

            // 封装为 PerProjectReport
            ProjectReportData.PerProjectReport perProject =
                    new ProjectReportData.PerProjectReport(brief, sections);

            ProjectReportData data = new ProjectReportData(
                    dateStart, dateEnd,
                    ReportDateUtils.rangeLabel(range),
                    ReportDateUtils.periodName(range),
                    Collections.singletonList(perProject));

            results.add(exportReport(tr, data, start));
        }

        long totalCost = System.currentTimeMillis() - totalStart;
        log.info("[ProjectReportStrategy] 全部完成，共 " + results.size()
                + " 份报告，总耗时 " + totalCost + "ms");
        return results;
    }


    // ========================================
    //  全项目汇总模式
    // ========================================

    private List<ReportResult> executeAllProjects(ReportRequest request) throws Exception {
        long totalStart = System.currentTimeMillis();

        // 1. 查询所有项目信息
        List<ProjectInfo> allProjectInfo = resolveAllProjects();
        if (allProjectInfo.isEmpty()) {
            throw new IllegalArgumentException("系统中没有可用的项目");
        }
        log.info("[ProjectReportStrategy] 全项目模式，共 "
                + allProjectInfo.size() + " 个项目");

        // 2. 构建 ProjectBrief 列表（全项目模式下每个项目都有自己的 brief）
        Map<String, ProjectReportData.ProjectBrief> briefMap = new LinkedHashMap<>();
        for (ProjectInfo pi : allProjectInfo) {
            briefMap.put(pi.name, new ProjectReportData.ProjectBrief(
                    pi.name, pi.director, pi.engineer, pi.address, pi.area));
        }

        List<ReportResult> results = new ArrayList<>();
        for (TimeRange tr : request.getTimeRanges()) {
            long start = System.currentTimeMillis();
            String range = tr.toReportRange();
            ReportDateUtils.DateRange dr = ReportDateUtils.getRange(range);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            String dateStart = sdf.format(dr.start);
            String dateEnd = sdf.format(dr.end);

            // 3. 查询所有数据源的全部数据（按日期范围，无项目过滤）
            Map<String, List<SearchFormDatasResponseBody.SearchFormDatasResponseBodyData>> allSourceData
                    = queryAllSourceData(dr.start, dr.end);

            // 4. 为每个项目构建独立的 PerProjectReport
            List<ProjectReportData.PerProjectReport> projectReports = new ArrayList<>();
            for (ProjectReportData.ProjectBrief brief : briefMap.values()) {
                List<ProjectReportData.SourceSection> sections =
                        buildSections(brief.getName(), allSourceData);
                projectReports.add(new ProjectReportData.PerProjectReport(brief, sections));
            }

            ProjectReportData data = new ProjectReportData(
                    dateStart, dateEnd,
                    ReportDateUtils.rangeLabel(range),
                    ReportDateUtils.periodName(range),
                    projectReports);

            results.add(exportReport(tr, data, start));
        }

        long totalCost = System.currentTimeMillis() - totalStart;
        log.info("[ProjectReportStrategy] 全部完成，共 " + results.size()
                + " 份报告，总耗时 " + totalCost + "ms");
        return results;
    }


    /**
     * 仅构建项目报告数据，不生成 PDF。
     * 用于两趟渲染：先获取数据，再由 ReportService 控制渲染流程。
     */
    public Map<String, Object> buildProjectReportData(ReportRequest request) throws Exception {
        Map<String, Object> result = new LinkedHashMap<>();

        boolean hasProjectParam = StringUtils.isNotBlank(request.getProjectId())
                || StringUtils.isNotBlank(request.getProjectName());

        if (hasProjectParam) {
            // 单项目模式
            ProjectInfo info = resolveProject(request.getProjectId(), request.getProjectName());
            if (info == null) throw new IllegalArgumentException("未找到项目");
            ProjectReportData.ProjectBrief brief = new ProjectReportData.ProjectBrief(
                    info.name, info.director, info.engineer, info.address, info.area);
            result.put("isMultiProject", false);
            // ... 简化为单项目模式
            return null;
        }

        // 全项目模式
        List<ProjectInfo> allProjectInfo = resolveAllProjects();
        if (allProjectInfo.isEmpty()) throw new IllegalArgumentException("系统中没有可用的项目");

        Map<String, ProjectReportData.ProjectBrief> briefMap = new LinkedHashMap<>();
        for (ProjectInfo pi : allProjectInfo) {
            briefMap.put(pi.name, new ProjectReportData.ProjectBrief(
                    pi.name, pi.director, pi.engineer, pi.address, pi.area));
        }

        List<ProjectReportData> dataList = new ArrayList<>();
        for (TimeRange tr : request.getTimeRanges()) {
            String range = tr.toReportRange();
            ReportDateUtils.DateRange dr = ReportDateUtils.getRange(range);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

            Map<String, List<SearchFormDatasResponseBody.SearchFormDatasResponseBodyData>> allSourceData
                    = queryAllSourceData(dr.start, dr.end);

            List<ProjectReportData.PerProjectReport> projectReports = new ArrayList<>();
            for (ProjectReportData.ProjectBrief brief : briefMap.values()) {
                List<ProjectReportData.SourceSection> sections =
                        buildSections(brief.getName(), allSourceData);
                projectReports.add(new ProjectReportData.PerProjectReport(brief, sections));
            }

            ProjectReportData data = new ProjectReportData(
                    sdf.format(dr.start), sdf.format(dr.end),
                    ReportDateUtils.rangeLabel(range),
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

    /**
     * 从 ProjectReportData 生成 HTML + PDF（底层 exportReport 委托）。
     */
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


    // ========================================
    //  构建数据源段落（按项目过滤）
    // ========================================

    /**
     * 为指定项目构建6个数据源段落。
     * 在全项目模式下，从全部数据中过滤出属于该项目的记录。
     *
     * @param projectName 项目名称
     * @param sourceData  所有数据源的全部记录
     * @return 该项目的 SourceSection 列表
     */
    private List<ProjectReportData.SourceSection> buildSections(
            String projectName,
            Map<String, List<SearchFormDatasResponseBody.SearchFormDatasResponseBodyData>> sourceData) {

        List<ProjectReportData.SourceSection> sections = new ArrayList<>();
        for (ReportConstants.SourceDef src : ReportConstants.SOURCES) {
            List<SearchFormDatasResponseBody.SearchFormDatasResponseBodyData> allRecords
                    = sourceData.getOrDefault(src.key, Collections.emptyList());

            // 按项目名称过滤记录
            List<SearchFormDatasResponseBody.SearchFormDatasResponseBodyData> projectRecords;
            if (projectName == null) {
                projectRecords = allRecords;
            } else {
                projectRecords = new ArrayList<>();
                for (SearchFormDatasResponseBody.SearchFormDatasResponseBodyData row : allRecords) {
                    Map<String, ?> fd = row.getFormData();
                    if (fd == null) continue;
                    String recProject = ReportHelper.extractField(fd, src.personField);
                    if (projectName.equals(recProject)) {
                        projectRecords.add(row);
                    }
                }
            }

            sections.add(new ProjectReportData.SourceSection(
                    src.key, src.label, src.color,
                    projectRecords.size(), extractFormDataList(projectRecords)));
        }
        return sections;
    }


    // ========================================
    //  项目信息查询
    // ========================================

    static class ProjectInfo {
        final String instId;
        final String name;
        final String director;
        final String engineer;
        final String address;
        final String area;

        ProjectInfo(String instId, String name, String director,
                    String engineer, String address, String area) {
            this.instId = instId;
            this.name = name;
            this.director = director;
            this.engineer = engineer;
            this.address = address;
            this.area = area;
        }
    }

    /**
     * 查询所有项目（排除测试项目），返回完整信息列表。
     */
    List<ProjectInfo> resolveAllProjects() throws Exception {
        List<SearchFormDatasResponseBody.SearchFormDatasResponseBodyData> raw =
                api.searchAllFormData(ReportConstants.FORM_PROJECT, null);
        List<ProjectInfo> result = new ArrayList<>();
        if (raw == null) return result;

        for (SearchFormDatasResponseBody.SearchFormDatasResponseBodyData row : raw) {
            Map<String, ?> fd = row.getFormData();
            if (fd == null) continue;
            String name = ReportHelper.extractField(fd, ReportConstants.F_PROJECT_NAME);
            if (StringUtils.isBlank(name)) continue;
            if (name.contains("测试") || name.toLowerCase().contains("test")) continue;

            String director = ReportHelper.extractField(fd, ReportConstants.F_PROJECT_DIRECTOR);
            String engineer = extractEngineer(fd);
            String addr = ReportHelper.extractField(fd, ReportConstants.F_PROJECT_ADDR);
            String area = ReportHelper.extractDistrict(addr);
            result.add(new ProjectInfo(row.getFormInstanceId(), name,
                    director != null ? director : "-",
                    engineer != null ? engineer : "-",
                    addr != null ? addr : "-",
                    area));
        }
        return result;
    }

    ProjectInfo resolveProject(String projectId, String projectName) throws Exception {
        List<ProjectInfo> all = resolveAllProjects();
        for (ProjectInfo pi : all) {
            if (projectId != null && projectId.equals(pi.instId)) return pi;
            if (projectName != null && projectName.equals(pi.name)) return pi;
        }
        return null;
    }

    private String extractEngineer(Map<String, ?> fd) {
        if (fd == null) return null;
        String val = ReportHelper.extractField(fd, "employeeField_mj803km0");
        return val != null ? val : "-";
    }


    // ========================================
    //  数据查询
    // ========================================

    /**
     * 按项目名称+日期范围并行查询6个数据源（单项目模式）。
     * 每条记录通过 personField 字段匹配项目名称。
     */
    private Map<String, List<SearchFormDatasResponseBody.SearchFormDatasResponseBodyData>>
            querySourceDataByProject(String projectName, Date start, Date end) throws Exception {

        Map<ReportConstants.SourceDef, List<SearchFormDatasResponseBody.SearchFormDatasResponseBodyData>> parallelResults =
                ReportParallel.parallelMap(
                        ReportConstants.SOURCES,
                        src -> {
                            try {
                                String sfj = ReportHelper.buildDateProjectFilter(
                                        src.dateField, src.personField, start, end, projectName);
                                List<SearchFormDatasResponseBody.SearchFormDatasResponseBodyData> records =
                                        api.searchAllFormData(src.formUuid, sfj);
                                return records != null ? records : Collections.emptyList();
                            } catch (Exception e) {
                                throw new RuntimeException("查询[" + src.label + "]失败: " + e.getMessage(), e);
                            }
                        },
                        "项目报告-单项目-6数据源查询"
                );

        Map<String, List<SearchFormDatasResponseBody.SearchFormDatasResponseBodyData>> result = new LinkedHashMap<>();
        for (Map.Entry<ReportConstants.SourceDef, List<SearchFormDatasResponseBody.SearchFormDatasResponseBodyData>> e : parallelResults.entrySet()) {
            result.put(e.getKey().key, e.getValue());
            log.info("[ProjectReportStrategy] 数据源 [" + e.getKey().label
                    + "] 完成，共 " + e.getValue().size() + " 条");
        }
        return result;
    }

    /**
     * 仅按日期范围并行查询6个数据源的全部数据（全项目模式）。
     * 全部数据返回后，由 buildSections() 按项目名称过滤。
     */
    private Map<String, List<SearchFormDatasResponseBody.SearchFormDatasResponseBodyData>>
            queryAllSourceData(Date start, Date end) throws Exception {

        Map<ReportConstants.SourceDef, List<SearchFormDatasResponseBody.SearchFormDatasResponseBodyData>> parallelResults =
                ReportParallel.parallelMap(
                        ReportConstants.SOURCES,
                        src -> {
                            try {
                                String sfj = ReportHelper.buildDateOnlyFilter(src.dateField, start, end);
                                List<SearchFormDatasResponseBody.SearchFormDatasResponseBodyData> records =
                                        api.searchAllFormData(src.formUuid, sfj);
                                return records != null ? records : Collections.emptyList();
                            } catch (Exception e) {
                                throw new RuntimeException("查询[" + src.label + "]失败: " + e.getMessage(), e);
                            }
                        },
                        "项目报告-全项目-6数据源查询"
                );

        Map<String, List<SearchFormDatasResponseBody.SearchFormDatasResponseBodyData>> result = new LinkedHashMap<>();
        for (Map.Entry<ReportConstants.SourceDef, List<SearchFormDatasResponseBody.SearchFormDatasResponseBodyData>> e : parallelResults.entrySet()) {
            result.put(e.getKey().key, e.getValue());
            log.info("[ProjectReportStrategy] 数据源 [" + e.getKey().label
                    + "] 完成，共 " + e.getValue().size() + " 条");
        }
        return result;
    }

    private List<Map<String, ?>> extractFormDataList(
            List<SearchFormDatasResponseBody.SearchFormDatasResponseBodyData> dataList) {
        if (dataList == null || dataList.isEmpty()) return Collections.emptyList();
        List<Map<String, ?>> result = new ArrayList<>(dataList.size());
        for (SearchFormDatasResponseBody.SearchFormDatasResponseBodyData row : dataList) {
            Map<String, ?> fd = row.getFormData();
            if (fd != null) result.add(fd);
        }
        return result;
    }
}
