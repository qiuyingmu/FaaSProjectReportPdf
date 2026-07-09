package com.alibaba.work.faas.report.model;

/**
 * 报告周期类型枚举 —— 替代旧的 PLATFORM/PROJECT 二元分类。
 *
 * <p>每个周期类型包含中文标签、时间范围代码、报告名称样式。</p>
 *
 * @author Senior Developer
 * 创建于 2026/07/09
 */
public enum ReportPeriod {

    WEEKLY("weekly", "周报"),
    MONTHLY("monthly", "月报"),
    QUARTERLY("quarterly", "季报");

    private final String code;
    private final String label;

    ReportPeriod(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() { return code; }
    public String getLabel() { return label; }

    /**
     * 根据 code 解析枚举，不区分大小写。
     */
    public static ReportPeriod fromCode(String code) {
        for (ReportPeriod p : values()) {
            if (p.code.equalsIgnoreCase(code)) return p;
        }
        throw new IllegalArgumentException("未知的周期类型: " + code
                + "，可选值: weekly, monthly, quarterly");
    }

    /**
     * 从中文标签解析。
     */
    public static ReportPeriod fromLabel(String label) {
        for (ReportPeriod p : values()) {
            if (p.label.equals(label)) return p;
        }
        throw new IllegalArgumentException("未知的周期类型: " + label
                + "，可选值: 周报, 月报, 季报");
    }
}
