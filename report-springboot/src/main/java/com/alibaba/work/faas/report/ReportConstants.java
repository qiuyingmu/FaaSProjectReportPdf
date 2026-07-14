package com.alibaba.work.faas.report;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 报表模块共享常量 —— 表单编码、字段 ID、数据源定义。
 *
 * <p>从 {@link ReportService} 中提取，供 {@link com.alibaba.work.faas.report.strategy.ReportStrategy} 实现类共享。</p>
 *
 * @author Senior Developer
 * 创建于 2026/07/07
 */
public final class ReportConstants {

    private ReportConstants() {}

    // ========================================
    //  宜搭表单编码（与 pt.js 完全一致）
    // ========================================

    /** 项目表单 */
    public static final String FORM_PROJECT = "FORM-6578CA890A0D42AEA1E3DFE3806A7C27K0BZ";

    /** 6 个数据源表单 */
    public static final String FORM_DOCLIB = "FORM-4ADCB90E94A44F22BC14265B222AE6E54P9S";
    public static final String FORM_DYNAMIC = "FORM-7F2A51B894CC4F348DC5C2B840772F36RHM3";
    public static final String FORM_LOG = "FORM-ACD427B1D88545F5BCC4B20DB6B4BD9FJVAX";
    public static final String FORM_SAFELOG = "FORM-7FC88BAA352945F29E30B337BCFD49C97TBT";
    public static final String FORM_STATION = "FORM-AF24B9440B5B41F1AC830065140D73FB1XBK";
    public static final String FORM_HAZARD = "FORM-04BDB63138D34DDE9A1330EEBD550473E4HJ";


    // ========================================
    //  字段 ID（与 pt.js F 对象完全一致）
    // ========================================

    // 项目表单字段
    public static final String F_PROJECT_NAME = "textField_mj7z6v5p";
    public static final String F_PROJECT_ADDR = "textField_mkktvlmv";
    public static final String F_PROJECT_DIRECTOR = "employeeField_mj803km2";

    // 各数据源的字段
    public static final String F_DOCLIB_PERSON = "textField_ml6no8vf";
    public static final String F_DOCLIB_DATE = "dateField_ml6no8wf";    // 提交日期（原dateField_ml6no8wb为资料日期）
    public static final String F_DYNAMIC_PERSON = "textField_mlg9av31";
    public static final String F_DYNAMIC_DATE = "dateField_mlelkrk3";
    public static final String F_LOG_PERSON = "textField_mjjngk77";
    public static final String F_LOG_DATE = "dateField_mkat24th";       // 提交日期
    public static final String F_SAFELOG_PERSON = "textField_mjjngk77";
    public static final String F_SAFELOG_DATE = "dateField_mkauls9o";   // 提交日期
    public static final String F_STATION_PERSON = "textField_mj89asvv";
    public static final String F_STATION_DATE = "dateField_mjjitv2s";   // 提交日期
    public static final String F_HAZARD_PERSON = "textField_mj89asvv";
    public static final String F_HAZARD_DATE = "dateField_mnzhv2ul";    // 提交日期


    // ========================================
    //  数据源定义（与 pt.js SRCS 完全一致）
    // ========================================

    /** 单个数据源定义 */
    public static class SourceDef {
        public final String key;
        public final String label;
        public final String color;
        public final String formUuid;
        public final String personField;
        public final String dateField;

        public SourceDef(String key, String label, String color, String formUuid,
                         String personField, String dateField) {
            this.key = key;
            this.label = label;
            this.color = color;
            this.formUuid = formUuid;
            this.personField = personField;
            this.dateField = dateField;
        }
    }

    /** 6 个数据源，顺序与 pt.js SRCS 一致 */
    public static final List<SourceDef> SOURCES = Collections.unmodifiableList(Arrays.asList(
            new SourceDef("docLib",  "资料库",      "#3b82f6", FORM_DOCLIB,  F_DOCLIB_PERSON,  F_DOCLIB_DATE),
            new SourceDef("dynamic", "项目动态",    "#10b981", FORM_DYNAMIC, F_DYNAMIC_PERSON, F_DYNAMIC_DATE),
            new SourceDef("log",     "监理日志",    "#f59e0b", FORM_LOG,     F_LOG_PERSON,     F_LOG_DATE),
            new SourceDef("safeLog", "日志(安全)",  "#8b5cf6", FORM_SAFELOG, F_SAFELOG_PERSON, F_SAFELOG_DATE),
            new SourceDef("station", "旁站记录",    "#ef4444", FORM_STATION, F_STATION_PERSON, F_STATION_DATE),
            new SourceDef("hazard",  "安全隐患",    "#14b8a6", FORM_HAZARD,  F_HAZARD_PERSON,  F_HAZARD_DATE)
    ));
}
