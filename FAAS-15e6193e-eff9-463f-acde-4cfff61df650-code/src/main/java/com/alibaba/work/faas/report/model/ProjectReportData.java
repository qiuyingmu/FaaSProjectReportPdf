package com.alibaba.work.faas.report.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 项目报告数据模型（v2 —— 按项目分组的完整报告）。
 *
 * <p>每个项目包含完整的独立报告：项目信息 + 6数据源统计 + 6数据源明细。
 * 全项目汇总模式下，每个项目一个区块，拼接成长报告。</p>
 *
 * @author Senior Developer
 * 创建于 2026/07/07
 */
public class ProjectReportData {

    // ========================================
    //  全局元数据
    // ========================================

    private final String dateStart;
    private final String dateEnd;
    private final String timeRangeLabel;
    private final String periodName;

    // ========================================
    //  按项目分组的报告列表
    // ========================================

    private final List<PerProjectReport> projectReports;

    // ========================================
    //  构造器
    // ========================================

    public ProjectReportData(String dateStart, String dateEnd,
                              String timeRangeLabel, String periodName,
                              List<PerProjectReport> projectReports) {
        this.dateStart = dateStart;
        this.dateEnd = dateEnd;
        this.timeRangeLabel = timeRangeLabel;
        this.periodName = periodName;
        this.projectReports = projectReports != null ? projectReports : Collections.emptyList();
    }

    // ========================================
    //  Getters
    // ========================================

    public String getDateStart() { return dateStart; }
    public String getDateEnd() { return dateEnd; }
    public String getTimeRangeLabel() { return timeRangeLabel; }
    public String getPeriodName() { return periodName; }
    public List<PerProjectReport> getProjectReports() { return projectReports; }

    /** 项目数量 */
    public int getProjectCount() { return projectReports.size(); }

    /** 总记录数（所有项目之和） */
    public int getTotalRecords() {
        return projectReports.stream()
                .flatMap(p -> p.getSources().stream())
                .mapToInt(s -> s.count)
                .sum();
    }

    /** 是否为单项目模式 */
    public boolean isSingleProject() { return projectReports.size() == 1; }

    /** 是否为全项目汇总模式 */
    public boolean isMultiProject() { return projectReports.size() > 1; }

    // ========================================
    //  单项目快捷访问（单项目模式时）
    // ========================================

    public PerProjectReport getSingleProject() {
        return isSingleProject() ? projectReports.get(0) : null;
    }

    // ========================================
    //  项目报告（每个项目独立）
    // ========================================

    public static class PerProjectReport {
        private final ProjectBrief brief;
        private final List<SourceSection> sources;

        public PerProjectReport(ProjectBrief brief, List<SourceSection> sources) {
            this.brief = brief;
            this.sources = sources != null ? sources : Collections.emptyList();
        }

        public ProjectBrief getBrief() { return brief; }
        public List<SourceSection> getSources() { return sources; }

        /** 该项目总记录数 */
        public int getTotalRecords() {
            return sources.stream().mapToInt(s -> s.count).sum();
        }
    }

    // ========================================
    //  项目简要信息
    // ========================================

    public static class ProjectBrief {
        private final String name;
        private final String director;
        private final String engineer;
        private final String address;
        private final String area;

        public ProjectBrief(String name, String director, String engineer,
                            String address, String area) {
            this.name = name;
            this.director = director;
            this.engineer = engineer;
            this.address = address;
            this.area = area;
        }

        public String getName() { return name; }
        public String getDirector() { return director; }
        public String getEngineer() { return engineer; }
        public String getAddress() { return address; }
        public String getArea() { return area; }
    }

    // ========================================
    //  数据源记录块
    // ========================================

    public static class SourceSection {
        public final String key;
        public final String label;
        public final String color;
        public final int count;
        public final List<Map<String, ?>> records;

        public SourceSection(String key, String label, String color,
                              int count, List<Map<String, ?>> records) {
            this.key = key;
            this.label = label;
            this.color = color;
            this.count = count;
            this.records = records != null ? records : Collections.emptyList();
        }

        public String getKey() { return key; }
        public String getLabel() { return label; }
        public String getColor() { return color; }
        public int getCount() { return count; }
        public List<Map<String, ?>> getRecords() { return records; }
    }
}
