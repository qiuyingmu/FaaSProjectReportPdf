package com.alibaba.work.faas.report.model;

/**
 * 报表类型枚举。
 *
 * <p>对应 FaaS 入参的「首要参数」——每次调用必带一个。</p>
 *
 * <ul>
 *   <li>{@link #PLATFORM} — 平台报告：概览所有项目</li>
 *   <li>{@link #PROJECT}  — 项目报告：单个项目深度统计（待实现）</li>
 * </ul>
 *
 * @author Senior Developer
 * 创建于 2026/07/07
 */
public enum ReportType {

    /** 平台报告 */
    PLATFORM("platform", "平台报告"),
    /** 项目报告 */
    PROJECT("project", "项目报告");

    private final String code;
    private final String label;

    ReportType(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() { return code; }
    public String getLabel() { return label; }

    /**
     * 根据 code 解析枚举，不区分大小写。
     *
     * @param code 编码（如 "platform"、"project"）
     * @return 匹配的枚举
     * @throws IllegalArgumentException 无法匹配时抛出
     */
    public static ReportType fromCode(String code) {
        for (ReportType t : values()) {
            if (t.code.equalsIgnoreCase(code)) return t;
        }
        throw new IllegalArgumentException("未知的报表类型: " + code
                + "，可选值: platform, project");
    }

    /**
     * 从中文标签解析（兼容宜搭参数可能传中文）。
     */
    public static ReportType fromLabel(String label) {
        for (ReportType t : values()) {
            if (t.label.equals(label)) return t;
        }
        throw new IllegalArgumentException("未知的报表类型: " + label
                + "，可选值: 平台报告, 项目报告");
    }
}
