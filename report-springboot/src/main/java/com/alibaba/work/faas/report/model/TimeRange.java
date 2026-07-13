package com.alibaba.work.faas.report.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 时间范围枚举（次要参数）。
 *
 * <p>对应 FaaS 入参的「次要参数」——每次调用可带零到多个。</p>
 *
 * @author Senior Developer
 * 创建于 2026/07/07
 */
public enum TimeRange {

    THIS_WEEK("thisWeek", "本周"),
    LAST_WEEK("lastWeek", "上周"),
    THIS_MONTH("thisMonth", "本月"),
    LAST_MONTH("lastMonth", "上月"),
    THIS_QUARTER("thisQuarter", "本季度"),
    LAST_QUARTER("lastQuarter", "上季度"),
    TODAY("today", "当日"),
    YESTERDAY("yesterday", "昨日");

    private final String code;
    private final String label;

    TimeRange(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() { return code; }
    public String getLabel() { return label; }

    /** 返回与 ReportService 兼容的 range 字符串 */
    public String toReportRange() {
        switch (this) {
            case THIS_WEEK:    return "week";
            case LAST_WEEK:    return "lastWeek";
            case THIS_MONTH:   return "month";
            case LAST_MONTH:   return "lastMonth";
            case THIS_QUARTER: return "quarter";
            case LAST_QUARTER: return "lastQuarter";
            case TODAY:        return "today";
            case YESTERDAY:    return "yesterday";
            default: throw new IllegalStateException("Unexpected: " + this);
        }
    }

    /**
     * 根据 code 解析枚举，不区分大小写。
     */
    public static TimeRange fromCode(String code) {
        for (TimeRange t : values()) {
            if (t.code.equalsIgnoreCase(code)) return t;
        }
        throw new IllegalArgumentException("未知的时间范围: " + code
                + "，可选值: thisWeek, lastWeek, thisMonth, lastMonth, thisQuarter, lastQuarter, today, yesterday");
    }

    /**
     * 从中文标签解析。
     */
    public static TimeRange fromLabel(String label) {
        for (TimeRange t : values()) {
            if (t.label.equals(label)) return t;
        }
        throw new IllegalArgumentException("未知的时间范围: " + label
                + "，可选值: 本周, 上周, 本月, 上月, 本季度, 上季度, 当日, 昨日");
    }

    /**
     * 将逗号/空格分隔的字符串解析为列表。
     * <p>支持 code 和 label 混合输入：{@code "thisWeek, 上周, lastMonth"}</p>
     *
     * @param raw 原始字符串（例如 "thisWeek, lastMonth"）
     * @return 时间范围列表
     */
    public static List<TimeRange> parseList(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            // 默认只返回本周
            List<TimeRange> result = new ArrayList<>();
            result.add(THIS_WEEK);
            return result;
        }

        String[] parts = raw.split("[,\\s]+");
        List<TimeRange> result = new ArrayList<>();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            try {
                result.add(fromCode(part));
            } catch (IllegalArgumentException e1) {
                try {
                    result.add(fromLabel(part));
                } catch (IllegalArgumentException e2) {
                    System.err.println("[TimeRange] 忽略无法识别的时间范围: " + part);
                }
            }
        }
        return result;
    }
}
