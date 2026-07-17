package com.alibaba.work.faas.report;

import com.alibaba.work.faas.report.model.ProjectReportData;
import com.alibaba.work.faas.service.YidaApiManager;
import com.aliyun.dingtalkyida_2_0.models.SearchFormDatasResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 报表查询服务 —— 消除 {@link PlatformReportStrategy} 和
 * {@link ProjectReportStrategy} 之间的重复查询逻辑。
 *
 * <p>统一提供：项目信息查询、6 个数据源并行查询、项目过滤、SourceSection 构建。</p>
 *
 * @author Senior Developer
 * 创建于 2026/07/09
 */
@Service
public class ReportQueryService {

    private static final Logger log = LoggerFactory.getLogger(ReportQueryService.class);

    private final YidaApiManager api;

    public ReportQueryService(YidaApiManager api) {
        this.api = api;
    }


    // ========================================
    //  项目信息
    // ========================================

    /** 项目基本信息（不含统计数据） */
    public static class ProjectInfo {
        public final String instId;
        public final String name;
        public final String director;      // 总监
        public final String engineer;       // 专监
        public final String address;
        public final String area;
        public final String chiefRepresentative; // 总监代表
        public final String inspector;          // 监理员

        public ProjectInfo(String instId, String name, String director,
                           String engineer, String address, String area,
                           String chiefRepresentative, String inspector) {
            this.instId = instId;
            this.name = name;
            this.director = director;
            this.engineer = engineer;
            this.address = address;
            this.area = area;
            this.chiefRepresentative = chiefRepresentative;
            this.inspector = inspector;
        }

        public ProjectReportData.ProjectBrief toBrief() {
            return new ProjectReportData.ProjectBrief(
                    name, director, engineer, address, area,
                    chiefRepresentative, inspector);
        }
    }

    /**
     * 查询所有项目（排除测试项目）。
     */
    public List<ProjectInfo> resolveAllProjects() throws Exception {
        List<SearchFormDatasResponseBody.SearchFormDatasResponseBodyData> raw =
                api.searchAllFormData(ReportConstants.FORM_PROJECT, null);
        List<ProjectInfo> result = new ArrayList<>();
        if (raw == null) return result;

        for (SearchFormDatasResponseBody.SearchFormDatasResponseBodyData row : raw) {
            Map<String, ?> fd = row.getFormData();
            if (fd == null) continue;
            String name = ReportHelper.extractField(fd, ReportConstants.F_PROJECT_NAME);
            if (name == null || name.isBlank()) continue;
            if (name.contains("测试") || name.toLowerCase().contains("test")) continue;

            String director = ReportHelper.extractField(fd, ReportConstants.F_PROJECT_DIRECTOR);
            String engineer = ReportHelper.extractField(fd, ReportConstants.F_PROJECT_ENGINEER);
            String chief = ReportHelper.extractField(fd, ReportConstants.F_PROJECT_CHIEF);
            String inspector = ReportHelper.extractField(fd, ReportConstants.F_PROJECT_INSPECTOR);
            String addr = ReportHelper.extractField(fd, ReportConstants.F_PROJECT_ADDR);
            String area = ReportHelper.extractDistrict(addr);
            result.add(new ProjectInfo(row.getFormInstanceId(), name,
                    director != null ? director : "-",
                    engineer != null ? engineer : "-",
                    addr != null ? addr : "-",
                    area,
                    chief,
                    inspector));
        }
        return result;
    }

    /** 按 ID 或名称查找单个项目。 */
    public ProjectInfo resolveProject(String projectId, String projectName) throws Exception {
        List<ProjectInfo> all = resolveAllProjects();
        for (ProjectInfo pi : all) {
            if (projectId != null && projectId.equals(pi.instId)) return pi;
            if (projectName != null && projectName.equals(pi.name)) return pi;
        }
        return null;
    }


    // ========================================
    //  数据源查询（并行 6 个数据源）
    // ========================================

    /**
     * 仅按日期范围查询 6 个数据源的全部数据。
     */
    public Map<String, List<SearchFormDatasResponseBody.SearchFormDatasResponseBodyData>>
            queryAllSourceData(Date start, Date end) throws Exception {
        return parallelQuerySources(src -> {
            try {
                String sfj = ReportHelper.buildDateOnlyFilter(src.dateField, start, end);
                List<SearchFormDatasResponseBody.SearchFormDatasResponseBodyData> records =
                        api.searchAllFormData(src.formUuid, sfj);
                return records != null ? records : Collections.emptyList();
            } catch (Exception e) {
                throw new RuntimeException("查询[" + src.label + "]失败: " + e.getMessage(), e);
            }
        }, "查询全部数据源");
    }

    /**
     * 按项目名称 + 日期范围并行查询 6 个数据源。
     */
    public Map<String, List<SearchFormDatasResponseBody.SearchFormDatasResponseBodyData>>
            querySourceDataByProject(String projectName, Date start, Date end) throws Exception {
        return parallelQuerySources(src -> {
            try {
                String sfj = ReportHelper.buildDateProjectFilter(
                        src.dateField, src.personField, start, end, projectName);
                List<SearchFormDatasResponseBody.SearchFormDatasResponseBodyData> records =
                        api.searchAllFormData(src.formUuid, sfj);
                return records != null ? records : Collections.emptyList();
            } catch (Exception e) {
                throw new RuntimeException("查询[" + src.label + "]失败: " + e.getMessage(), e);
            }
        }, "按项目[" + projectName + "]查询数据源");
    }

    /**
     * 并行统计每个项目在 6 个数据源中的记录数（平台报告用）。
     */
    public Map<String, Map<String, Integer>> loadSourceCounts(Date start, Date end) throws Exception {
        Map<ReportConstants.SourceDef, Map<String, Integer>> parallelResults =
                ReportParallel.parallelMap(
                        ReportConstants.SOURCES,
                        src -> {
                            try {
                                return loadSingleSourceCounts(src, start, end);
                            } catch (Exception e) {
                                throw new RuntimeException("查询数据源[" + src.label + "]失败: " + e.getMessage(), e);
                            }
                        },
                        "平台报告-6数据源统计"
                );

        Map<String, Map<String, Integer>> result = new LinkedHashMap<>();
        for (Map.Entry<ReportConstants.SourceDef, Map<String, Integer>> entry : parallelResults.entrySet()) {
            String key = entry.getKey().key;
            result.put(key, entry.getValue());
            int total = entry.getValue().values().stream().mapToInt(Integer::intValue).sum();
            log.debug("[ReportQueryService] 数据源 [{}] 完成，共 {} 条，涉及 {} 个项目",
                    entry.getKey().label, total, entry.getValue().size());
        }
        return result;
    }

    /** 查询单个数据源并按项目名称聚合记录数。 */
    private Map<String, Integer> loadSingleSourceCounts(ReportConstants.SourceDef src,
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

    /** 并行查询 6 个数据源的通用模板方法。 */
    private Map<String, List<SearchFormDatasResponseBody.SearchFormDatasResponseBodyData>>
            parallelQuerySources(
            java.util.function.Function<ReportConstants.SourceDef,
                    List<SearchFormDatasResponseBody.SearchFormDatasResponseBodyData>> queryFn,
            String taskName) throws Exception {

        Map<ReportConstants.SourceDef, List<SearchFormDatasResponseBody.SearchFormDatasResponseBodyData>> parallelResults =
                ReportParallel.parallelMap(ReportConstants.SOURCES, queryFn, taskName);

        Map<String, List<SearchFormDatasResponseBody.SearchFormDatasResponseBodyData>> result = new LinkedHashMap<>();
        for (Map.Entry<ReportConstants.SourceDef, List<SearchFormDatasResponseBody.SearchFormDatasResponseBodyData>> e : parallelResults.entrySet()) {
            result.put(e.getKey().key, e.getValue());
            log.debug("[ReportQueryService] 数据源 [{}] 完成，共 {} 条", e.getKey().label, e.getValue().size());
        }
        return result;
    }


    // ========================================
    //  数据源段落构建（项目报告用）
    // ========================================

    /**
     * 为指定项目从全量数据中过滤出属于该项目的记录，构建 6 个 SourceSection。
     */
    public List<ProjectReportData.SourceSection> buildSections(
            String projectName,
            Map<String, List<SearchFormDatasResponseBody.SearchFormDatasResponseBodyData>> sourceData) {

        List<ProjectReportData.SourceSection> sections = new ArrayList<>();
        for (ReportConstants.SourceDef src : ReportConstants.SOURCES) {
            List<SearchFormDatasResponseBody.SearchFormDatasResponseBodyData> allRecords
                    = sourceData.getOrDefault(src.key, Collections.emptyList());

            List<SearchFormDatasResponseBody.SearchFormDatasResponseBodyData> projectRecords;
            if (projectName == null) {
                projectRecords = allRecords;
            } else {
                projectRecords = allRecords.stream()
                        .filter(row -> {
                            Map<String, ?> fd = row.getFormData();
                            if (fd == null) return false;
                            String recProject = ReportHelper.extractField(fd, src.personField);
                            return projectName.equals(recProject);
                        })
                        .collect(Collectors.toList());
            }

            // 按日期降序排序（最新在前），日期相同时按 formInstanceId 稳定排序
            projectRecords.sort((a, b) -> {
                long da = getFormDataDate(a, src.dateField);
                long db = getFormDataDate(b, src.dateField);
                int cmp = Long.compare(db, da); // 降序
                if (cmp != 0) return cmp;
                return String.valueOf(a.getFormInstanceId()).compareTo(
                        String.valueOf(b.getFormInstanceId()));
            });

            sections.add(new ProjectReportData.SourceSection(
                    src.key, src.label, src.color,
                    projectRecords.size(), extractFormDataList(projectRecords)));
        }
        return sections;
    }

    /** 提取记录中日期字段的时间戳（毫秒），用于排序。找不到时返回 0。 */
    private static long getFormDataDate(
            SearchFormDatasResponseBody.SearchFormDatasResponseBodyData row, String dateField) {
        if (row == null || row.getFormData() == null) return 0;
        Object val = row.getFormData().get(dateField);
        if (val == null) return 0;
        // 1. 数字（时间戳毫秒）
        if (val instanceof Number) return ((Number) val).longValue();
        // 2. 字符串（数字或 ISO 日期）
        if (val instanceof String) {
            String s = ((String) val).trim();
            if (s.isEmpty()) return 0;
            // 尝试作为毫秒时间戳
            try { return Long.parseLong(s); } catch (NumberFormatException ignored) {}
            // 尝试解析 ISO 日期 (yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss)
            try {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
                return sdf.parse(s.substring(0, Math.min(10, s.length()))).getTime();
            } catch (Exception ignored) {}
        }
        // 3. JSONArray（宜搭日期字段有时是数组）
        if (val instanceof java.util.List) {
            java.util.List<?> list = (java.util.List<?>) val;
            for (Object item : list) {
                if (item == null) continue;
                if (item instanceof Number) return ((Number) item).longValue();
                if (item instanceof String) {
                    try { return Long.parseLong((String) item); } catch (NumberFormatException ignored) {}
                }
            }
        }
        return 0;
    }

    /** 从 SearchFormDatasResponseBody 列表中提取 formData Map。 */
    public static List<Map<String, ?>> extractFormDataList(
            List<SearchFormDatasResponseBody.SearchFormDatasResponseBodyData> dataList) {
        if (dataList == null || dataList.isEmpty()) return Collections.emptyList();
        return dataList.stream()
                .map(SearchFormDatasResponseBody.SearchFormDatasResponseBodyData::getFormData)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
