package com.alibaba.work.faas.report;

import java.util.*;

/**
 * 报表辅助方法 —— 字段提取、地址解析等。
 *
 * <p>从 {@link ReportService} 中提取，对应 pt.js 的 ev()、extractDistrict() 等函数。
 * 所有方法均为静态，线程安全。</p>
 *
 * @author Senior Developer
 * 创建于 2026/07/07
 */
public final class ReportHelper {

    private ReportHelper() {}

    /**
     * 从 formData 中提取字段值。
     * 对应 pt.js ev()。宜搭 API 返回的 formData 可能是普通值或对象结构。
     */
    @SuppressWarnings("unchecked")
    public static String extractField(Map<String, ?> formData, String fieldId) {
        if (formData == null || fieldId == null) return null;
        Object val = formData.get(fieldId);
        if (val == null) return null;

        // 简单类型
        if (val instanceof String) return (String) val;
        if (val instanceof Number) return val.toString();

        // 对象类型 {value: ..., ...}
        if (val instanceof Map) {
            Map<String, Object> m = (Map<String, Object>) val;
            Object v = m.get("value");
            if (v instanceof List) {
                List<Object> list = (List<Object>) v;
                StringBuilder sb = new StringBuilder();
                for (Object item : list) {
                    if (sb.length() > 0) sb.append("、");
                    if (item instanceof Map) {
                        Map<String, Object> itemMap = (Map<String, Object>) item;
                        Object name = itemMap.get("name");
                        sb.append(name != null ? name.toString() : item.toString());
                    } else {
                        sb.append(item.toString());
                    }
                }
                return sb.toString();
            }
            if (v != null) {
                if (v instanceof Map) {
                    Map<String, Object> vm = (Map<String, Object>) v;
                    Object label = vm.get("label");
                    return label != null ? label.toString() : vm.toString();
                }
                return v.toString();
            }
            Object label = m.get("label");
            return label != null ? label.toString() : m.toString();
        }

        String raw = val.toString();

        // 清洗：去除 Yida 组织成员字段可能带来的方括号包裹（如 "[国光军]" → "国光军"）
        if (raw.startsWith("[") && raw.endsWith("]")) {
            raw = raw.substring(1, raw.length() - 1).trim();
        }
        return raw;
    }

    /**
     * 从级联字段提取完整路径（对应 xm.js getCascadeLabel）。
     *
     * <p>宜搭 cascadeSelectField 的 value 是数组，如：
     * <code>["监理业务", "施工单位", "特种作业人员操作证书及报审表"]</code> 或
     * <code>[{label:"监理业务", key:"..."}, {label:"施工单位", ...}]</code>。
     * 本方法将各级用 <code> > </code> 连接，返回完整路径。</p>
     *
     * @param formData formData 对象
     * @param fieldId  字段 ID（如 <code>cascadeSelectField_ml6no8w5</code>）
     * @return 级联路径字符串，如 <code>"监理业务 > 施工单位 > 特种作业人员操作证书及报审表"</code>；若为空则返回 <code>"未分类"</code>
     */
    @SuppressWarnings("unchecked")
    public static String extractCascadeLabel(Map<String, ?> formData, String fieldId) {
        if (formData == null || fieldId == null) return "未分类";
        Object f = formData.get(fieldId);
        if (f == null) return "未分类";

        // 取 value（若 f 是对象）或直接取 f
        Object val;
        if (f instanceof Map) {
            Map<String, Object> m = (Map<String, Object>) f;
            val = m.containsKey("value") ? m.get("value") : m;
        } else {
            val = f;
        }

        if (val == null || "".equals(val)) return "未分类";

        // 统一转为数组
        List<Object> list;
        if (val instanceof List) {
            list = (List<Object>) val;
        } else {
            list = Collections.singletonList(val);
        }

        StringBuilder sb = new StringBuilder();
        for (Object item : list) {
            if (item == null || "".equals(item)) continue;
            if (sb.length() > 0) sb.append(" > ");
            if (item instanceof Map) {
                Map<String, Object> m = (Map<String, Object>) item;
                Object key = m.get("key");
                Object label = m.get("label");
                if (key != null && !key.toString().isEmpty()) {
                    sb.append(key.toString());
                } else if (label != null) {
                    sb.append(label.toString());
                } else {
                    sb.append(m.toString());
                }
            } else {
                sb.append(item.toString());
            }
        }

        return sb.length() > 0 ? sb.toString() : "未分类";
    }

    /**
     * 从地址中提取区域名（对应 pt.js extractDistrict()）。
     */
    public static String extractDistrict(String addr) {
        if (addr == null) return "-";
        java.util.regex.Pattern p1 = java.util.regex.Pattern.compile("([^\\s,，、]+?区)");
        java.util.regex.Matcher m1 = p1.matcher(addr);
        if (m1.find()) return m1.group(1);

        java.util.regex.Pattern p2 = java.util.regex.Pattern.compile("([^\\s,，、]+?县)(?![区市])");
        java.util.regex.Matcher m2 = p2.matcher(addr);
        if (m2.find()) return m2.group(1);

        List<String> cities = new ArrayList<>();
        java.util.regex.Pattern p3 = java.util.regex.Pattern.compile("([^\\s,，、]+?市)");
        java.util.regex.Matcher m3 = p3.matcher(addr);
        while (m3.find()) cities.add(m3.group(1));
        if (cities.size() >= 2) return cities.get(cities.size() - 2);
        if (cities.size() == 1) return cities.get(0);

        return addr.length() > 6 ? addr.substring(0, 6) + "…" : addr;
    }

    /**
     * 构建仅含日期范围的 searchFieldJson（不含项目过滤）。
     */
    @SuppressWarnings("unchecked")
    public static String buildDateOnlyFilter(String dateField, Date start, Date end) {
        Map<String, Object> filter = new LinkedHashMap<>();
        filter.put(dateField, Arrays.asList(
                ReportDateUtils.dayStart(start), ReportDateUtils.dayEnd(end)));
        return com.alibaba.fastjson.JSON.toJSONString(filter);
    }

    /**
     * 构建 searchFieldJson，按日期范围 + 项目名称过滤。
     */
    @SuppressWarnings("unchecked")
    public static String buildDateProjectFilter(String dateField, String projectField,
                                                 Date start, Date end, String projectName) {
        Map<String, Object> filter = new LinkedHashMap<>();
        filter.put(dateField, Arrays.asList(
                ReportDateUtils.dayStart(start), ReportDateUtils.dayEnd(end)));
        filter.put(projectField, projectName);
        return com.alibaba.fastjson.JSON.toJSONString(filter);
    }
}
