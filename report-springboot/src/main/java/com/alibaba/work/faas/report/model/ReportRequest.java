package com.alibaba.work.faas.report.model;

import java.util.List;
import java.util.Map;

/**
 * 报表请求模型。
 *
 * <p>封装 FaaS 连接器传入的参数，经 {@link #fromInput(Map)} 工厂方法从 {@code Map<String,Object>} 解析。
 * 每次请求包含一个「首要参数」（报表类型）和零到多个「次要参数」（时间范围）。</p>
 *
 * <h3>入参格式约定（与宜搭集成自动化的参数映射匹配）</h3>
 * <pre>
 * {
 *   "reportType": "platform",          // 或 "project"
 *   "timeRanges": "thisWeek,lastMonth",// 逗号分隔，可选，默认本周
 *   "projectId": "INST-xxxx"           // 项目报告时必填
 * }
 * </pre>
 *
 * @author Senior Developer
 * 创建于 2026/07/07
 */
public class ReportRequest {

    /** 报表类型（必填） */
    private final ReportType type;

    /** 时间范围列表（必填，至少一个） */
    private final List<TimeRange> timeRanges;

    /** 项目实例 ID（项目报告时必填） */
    private final String projectId;

    /** 项目名称（项目报告时可选，用于展示） */
    private final String projectName;

    /** 周期标签（周报/月报/季报），平台报告时由调用方传入以消除推断歧义 */
    private final String periodLabel;

    public ReportRequest(ReportType type, List<TimeRange> timeRanges,
                         String projectId, String projectName) {
        this(type, timeRanges, projectId, projectName, null);
    }

    public ReportRequest(ReportType type, List<TimeRange> timeRanges,
                         String projectId, String projectName, String periodLabel) {
        if (type == null) throw new IllegalArgumentException("报表类型不能为空");
        if (timeRanges == null || timeRanges.isEmpty()) {
            throw new IllegalArgumentException("时间范围不能为空");
        }
        this.type = type;
        this.timeRanges = timeRanges;
        this.projectId = projectId;
        this.projectName = projectName;
        this.periodLabel = periodLabel;
    }

    // ========================================
    //  Getters
    // ========================================

    public ReportType getType() { return type; }
    public List<TimeRange> getTimeRanges() { return timeRanges; }
    public String getProjectId() { return projectId; }
    public String getProjectName() { return projectName; }
    public String getPeriodLabel() { return periodLabel; }

    /** 是否为项目报告 */
    public boolean isProjectReport() { return type == ReportType.PROJECT; }

    /** 是否为平台报告 */
    public boolean isPlatformReport() { return type == ReportType.PLATFORM; }


    // ========================================
    //  从 FaaS Map 输入解析
    // ========================================

    /** FaaS 参数 key */
    private static final String KEY_TYPE = "reportType";
    private static final String KEY_RANGES = "timeRanges";
    private static final String KEY_PROJECT_ID = "projectId";
    private static final String KEY_PROJECT_NAME = "projectName";

    /**
     * 从 FaaS 连接器传入的 {@code Map<String, Object>} 解析请求参数。
     *
     * <p>兼容中文/英文参数名，兼容字符串/数组格式的时间范围。</p>
     *
     * @param input FaaS 的 input Map（来自 {@code faasInputs.getInputs()}）
     * @return 报表请求对象
     */
    @SuppressWarnings("unchecked")
    public static ReportRequest fromInput(Map<String, Object> input) {
        if (input == null || input.isEmpty()) {
            throw new IllegalArgumentException("FaaS 入参不能为空");
        }

        // ---- 1. 解析首要参数：报表类型 ----
        String typeStr = getString(input, KEY_TYPE, "reportType", "报表类型");
        ReportType type;
        try {
            type = ReportType.fromCode(typeStr);
        } catch (IllegalArgumentException e1) {
            type = ReportType.fromLabel(typeStr);
        }

        // ---- 2. 解析次要参数：时间范围 ----
        String rangesStr = getString(input, KEY_RANGES, "timeRanges", "timeRange", "时间范围");
        List<TimeRange> timeRanges = TimeRange.parseList(rangesStr);

        // ---- 3. 项目特有参数 ----
        String projectId = getString(input, KEY_PROJECT_ID, "projectId", "项目ID");
        String projectName = getString(input, KEY_PROJECT_NAME, "projectName", "项目名称");

        return new ReportRequest(type, timeRanges, projectId, projectName);
    }

    /**
     * 从 Map 中按多个候选 key 依次取值，返回第一个非空字符串。
     */
    private static String getString(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            if (map.containsKey(key)) {
                Object val = map.get(key);
                if (val != null) {
                    String s = val.toString().trim();
                    if (!s.isEmpty()) return s;
                }
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "ReportRequest{type=" + type.getLabel()
                + ", timeRanges=" + timeRanges
                + (projectId != null ? ", projectId=" + projectId : "")
                + (projectName != null ? ", projectName=" + projectName : "")
                + "}";
    }
}
