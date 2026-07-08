package com.alibaba.work.faas.report;

import java.util.List;
import java.util.Map;

/**
 * 报表数据模型。
 * 对应 pt.js 中的 _P（项目列表）、_stats（统计结果）、SRCS（数据源定义）。
 */
public class ReportData {

    /** 时间范围标签，如：第27周（6月29日~7月5日） */
    private final String timeRangeLabel;

    /** 时间范围名称，如：本周/上周/本月/上月 */
    private final String periodName;

    /** 时间范围标识 */
    private final String rangeKey;

    /** 项目列表 */
    private final List<ProjectStat> projects;

    /** 总记录数 */
    private final int totalRecords;

    public ReportData(String timeRangeLabel, String periodName, String rangeKey,
                      List<ProjectStat> projects, int totalRecords) {
        this.timeRangeLabel = timeRangeLabel;
        this.periodName = periodName;
        this.rangeKey = rangeKey;
        this.projects = projects;
        this.totalRecords = totalRecords;
    }

    public String getTimeRangeLabel() { return timeRangeLabel; }
    public String getPeriodName() { return periodName; }
    public String getRangeKey() { return rangeKey; }
    public List<ProjectStat> getProjects() { return projects; }
    public int getTotalRecords() { return totalRecords; }


    // ========================================
    //  单项目统计
    // ========================================

    /**
     * 对应 pt.js 中每个项目的统计数据。
     * _P[i] + _stats[projectName]
     */
    public static class ProjectStat {
        private final String instId;
        private final String name;
        private final String director;
        private final String area;
        /** 6 个数据源各自的条数 key=sourceKey（docLib/dynamic/log/safeLog/station/hazard） */
        private final Map<String, Integer> sourceCounts;
        private final int totalCount;

        public ProjectStat(String instId, String name, String director, String area,
                           Map<String, Integer> sourceCounts, int totalCount) {
            this.instId = instId;
            this.name = name;
            this.director = director;
            this.area = area;
            this.sourceCounts = sourceCounts;
            this.totalCount = totalCount;
        }

        public String getInstId() { return instId; }
        public String getName() { return name; }
        public String getDirector() { return director; }
        public String getArea() { return area; }
        public Map<String, Integer> getSourceCounts() { return sourceCounts; }
        public int getTotalCount() { return totalCount; }
    }
}
