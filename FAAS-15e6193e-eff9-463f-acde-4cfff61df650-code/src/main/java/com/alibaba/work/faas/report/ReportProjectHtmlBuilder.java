package com.alibaba.work.faas.report;

import com.alibaba.work.faas.report.model.ProjectReportData;
import com.alibaba.work.faas.report.model.ProjectReportData.PerProjectReport;
import com.alibaba.work.faas.report.model.ProjectReportData.SourceSection;
import com.alibaba.work.faas.report.model.ProjectReportData.ProjectBrief;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 项目报告 HTML 生成器（浏览器版 — xm.js 风格）。
 *
 * <p>每个项目生成独立的完整区块：项目信息卡片 + 统计卡片 + 6数据源明细表。
 * 全项目汇总模式下，按项目顺序拼接各项目区块。
 * 无项目筛选 UI。</p>
 *
 * <p>对应 xm.js renderJsx() 的渲染逻辑。
 * 使用 modern CSS（flexbox/grid/gradient/border-radius），
 * 供浏览器直接查看。</p>
 *
 * @author Senior Developer
 * 创建于 2026/07/07
 */
public class ReportProjectHtmlBuilder {

    public static final ReportProjectHtmlBuilder INSTANCE = new ReportProjectHtmlBuilder();
    private ReportProjectHtmlBuilder() {}


    // ========================================
    //  数据源常量（与 xm.js srcKeys / srcNames 对应）
    // ========================================

    private static final String[] SRC_KEYS = {"docLib", "dynamic", "log", "safeLog", "station", "hazard"};
    private static final String[] SRC_NAMES = {"资料库", "项目动态", "监理日志", "日志(安全)", "旁站记录", "安全隐患台账"};
    private static final String[] SRC_COLORS = {"#3b82f6", "#10b981", "#f59e0b", "#8b5cf6", "#ef4444", "#14b8a6"};


    // ========================================
    //  入口
    // ========================================

    /**
     * 构建完整项目报告 HTML。
     */
    public String build(ProjectReportData data) {
        StringBuilder html = new StringBuilder(16384);
        String title = data.isMultiProject()
                ? "全项目汇总报告"
                : "项目报告";
        html.append("<!DOCTYPE html>\n")
            .append("<html lang=\"zh-CN\">\n<head>\n")
            .append("<meta charset=\"UTF-8\"/>\n")
            .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"/>\n")
            .append("<title>").append(title).append("</title>\n")
            .append("<style type=\"text/css\">\n").append(CSS).append("</style>\n")
            .append("</head>\n<body>\n")
            .append("<div class=\"container\">\n")
            .append(buildHeader(data));

        // 遍历每个项目，生成独立区块
        for (int i = 0; i < data.getProjectReports().size(); i++) {
            PerProjectReport pr = data.getProjectReports().get(i);
            html.append(buildProjectBlock(pr, data, i + 1));
        }

        html.append(buildFooter())
            .append("</div>\n</body>\n</html>");
        return html.toString();
    }


    // ========================================
    //  头部
    // ========================================

    private String buildHeader(ProjectReportData data) {
        String badge = data.isMultiProject() ? "\uD83D\uDCCA 全项目汇总报告" : "\uD83D\uDCCA 项目报告";
        String subtitle = data.isMultiProject()
                ? "【全项目汇总报告-" + data.getTimeRangeLabel() + "】"
                : "【项目报告-" + data.getTimeRangeLabel() + "】";
        return ""
            + "<div class=\"report-header\">\n"
            + "  <div class=\"header-badge\">" + badge + "</div>\n"
            + "  <h1>" + subtitle + "</h1>\n"
            + "  <div class=\"subtitle\">报告生成时间：" + nowStr()
            + " \uFF5C 统计范围：" + data.getDateStart() + " ~ " + data.getDateEnd()
            + (data.isMultiProject() ? " \uFF5C 共 " + data.getProjectCount() + " 个项目" : "")
            + "</div>\n"
            + "</div>\n";
    }


    // ========================================
    //  项目区块
    // ========================================

    /**
     * 构建单个项目的完整区块。
     * 顺序：项目序号标题 → 项目信息 → 统计 → 6数据源明细。
     */
    private String buildProjectBlock(PerProjectReport pr, ProjectReportData data, int index) {
        ProjectBrief brief = pr.getBrief();
        List<SourceSection> sources = pr.getSources();

        StringBuilder sb = new StringBuilder(4096);

        // 项目序号标题
        sb.append("<div class=\"project-title-bar\">\n")
          .append("  <span class=\"project-index\">").append(index).append("</span>\n")
          .append("  <span class=\"project-name-title\">").append(escHtml(brief.getName())).append("</span>\n")
          .append("  <span class=\"badge\">").append(pr.getTotalRecords()).append(" 条</span>\n")
          .append("</div>\n");

        // 项目信息卡片
        sb.append(buildProjectInfo(brief, data));

        // 统计卡片
        sb.append(buildStats(sources, data));

        // 6数据源明细
        sb.append(buildSourceSections(sources));

        return sb.toString();
    }


    // ========================================
    //  项目信息卡片（对应 xm.js 项目信息区块）
    // ========================================

    private String buildProjectInfo(ProjectBrief brief, ProjectReportData data) {
        return ""
            + "<div class=\"section-card\">\n"
            + "  <div class=\"section-title\">\uD83D\uDCCB 项目信息</div>\n"
            + "  <div class=\"info-grid\">\n"
            + "    <div class=\"info-item\">\n"
            + "      <span class=\"info-label\">报告时间</span>\n"
            + "      <span class=\"info-value\">" + data.getDateStart() + " ~ " + data.getDateEnd() + "</span>\n"
            + "    </div>\n"
            + "    <div class=\"info-item\">\n"
            + "      <span class=\"info-label\">项目总监</span>\n"
            + "      <span class=\"info-value\">" + escHtml(brief.getDirector()) + "</span>\n"
            + "    </div>\n"
            + "    <div class=\"info-item\">\n"
            + "      <span class=\"info-label\">专监</span>\n"
            + "      <span class=\"info-value\">" + escHtml(brief.getEngineer()) + "</span>\n"
            + "    </div>\n"
            + "    <div class=\"info-item\">\n"
            + "      <span class=\"info-label\">项目地址</span>\n"
            + "      <span class=\"info-value\">" + escHtml(brief.getAddress()) + "</span>\n"
            + "    </div>\n"
            + "    <div class=\"info-item\">\n"
            + "      <span class=\"info-label\">人员</span>\n"
            + "      <span class=\"info-value person-tags\">\n"
            + "        <span class=\"person-tag director-tag\">" + escHtml(brief.getDirector()) + "（总监）</span>\n"
            + "        <span class=\"person-tag engineer-tag\">" + escHtml(brief.getEngineer()) + "（专监）</span>\n"
            + "      </span>\n"
            + "    </div>\n"
            + "  </div>\n"
            + "</div>\n";
    }


    // ========================================
    //  统计卡片（对应 xm.js 各数据源统计区块）
    // ========================================

    private String buildStats(List<SourceSection> sources, ProjectReportData data) {
        // 按 SRC_KEYS 顺序查找对应的统计数
        Map<String, SourceSection> srcMap = new HashMap<>();
        for (SourceSection s : sources) {
            srcMap.put(s.getKey(), s);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"section-card\">\n")
          .append("  <div class=\"section-title\">\uD83D\uDCCA 各数据源统计</div>\n")
          .append("  <div class=\"stats-grid\">\n");

        for (int i = 0; i < SRC_KEYS.length; i++) {
            String key = SRC_KEYS[i];
            SourceSection src = srcMap.get(key);
            int count = (src != null) ? src.getCount() : 0;
            sb.append("    <div class=\"stat-card\">\n")
              .append("      <div class=\"stat-label\">").append(SRC_NAMES[i]).append("</div>\n")
              .append("      <div class=\"stat-count\" style=\"color:").append(SRC_COLORS[i]).append(";\">")
              .append(count).append("</div>\n")
              .append("    </div>\n");
        }

        sb.append("  </div>\n</div>\n");
        return sb.toString();
    }


    // ========================================
    //  6数据源明细（可折叠）
    // ========================================

    private String buildSourceSections(List<SourceSection> sources) {
        Map<String, SourceSection> srcMap = new HashMap<>();
        for (SourceSection s : sources) {
            srcMap.put(s.getKey(), s);
        }

        StringBuilder sb = new StringBuilder();

        // 按 xm.js 的顺序：docLib, dynamic, log, safeLog, station, hazard
        for (String key : SRC_KEYS) {
            SourceSection src = srcMap.get(key);
            if (src == null || src.getRecords().isEmpty()) {
                sb.append(buildEmptySection(key));
                continue;
            }
            sb.append(buildSourceSection(key, src));
        }

        return sb.toString();
    }


    /**
     * 构建单个数据源的可折叠区块（使用 details/summary 实现折叠效果）。
     */
    private String buildSourceSection(String key, SourceSection src) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"source-section\">\n")
          .append("  <details class=\"source-details\" open>\n")
          .append("    <summary class=\"source-summary\">\n")
          .append("      <span class=\"source-summary-left\">")
          .append(getSourceEmoji(key)).append(" ").append(src.getLabel())
          .append(" <span class=\"badge\">").append(src.getCount()).append(" 条</span></span>\n")
          .append("      <span class=\"collapse-arrow\">\u25BC</span>\n")
          .append("    </summary>\n");

        if ("docLib".equals(key)) {
            sb.append(buildDocLibContent(src));
        } else if ("hazard".equals(key)) {
            sb.append(buildHazardContent(src));
        } else if ("dynamic".equals(key)) {
            sb.append(buildDynamicContent(src));
        } else {
            sb.append(buildSimpleContent(src, key));
        }

        sb.append("  </details>\n</div>\n");
        return sb.toString();
    }


    /**
     * 资料库：按分类分组（对应 xm.js 中按 cascadeSelectField 分组）。
     */
    private String buildDocLibContent(SourceSection src) {
        // 按分类分组
        Map<String, List<Map<String, ?>>> groups = new LinkedHashMap<>();
        for (Map<String, ?> fd : src.getRecords()) {
            String cat = ReportHelper.extractCascadeLabel(fd, "cascadeSelectField_ml6no8w5");
            if (cat == null || cat.isEmpty()) cat = "未分类";
            groups.computeIfAbsent(cat, k -> new ArrayList<>()).add(fd);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("    <div class=\"section-content\">\n");

        for (Map.Entry<String, List<Map<String, ?>>> entry : groups.entrySet()) {
            String cat = entry.getKey();
            List<Map<String, ?>> items = entry.getValue();

            sb.append("      <div class=\"sub-section\">\n")
              .append("        <div class=\"sub-title\">").append(escHtml(cat))
              .append(" <span class=\"badge\">").append(items.size()).append(" 条</span></div>\n")
              .append("        <table class=\"data-table\">\n")
              .append("          <thead><tr><th>提交人</th><th>日期</th></tr></thead>\n")
              .append("          <tbody>\n");

            for (Map<String, ?> fd : items) {
                String submitter = getEmpName(fd, "employeeField_ml6no8vd");
                String date = getDateStr(fd, "dateField_ml6no8wb");
                sb.append("            <tr><td>").append(escHtml(submitter))
                  .append("</td><td>").append(escHtml(date)).append("</td></tr>\n");
            }

            sb.append("          </tbody>\n</table>\n")
              .append("      </div>\n");
        }

        sb.append("    </div>\n");
        return sb.toString();
    }


    /**
     * 项目动态：提交人 + 标题/内容 + 日期
     */
    private String buildDynamicContent(SourceSection src) {
        StringBuilder sb = new StringBuilder();
        sb.append("    <div class=\"section-content\">\n")
          .append("      <table class=\"data-table\">\n")
          .append("        <thead><tr><th>提交人</th><th>标题/内容</th><th>日期</th></tr></thead>\n")
          .append("        <tbody>\n");

        for (Map<String, ?> fd : src.getRecords()) {
            String submitter = getEmpName(fd, "employeeField_mlenlbgs");
            String title = ReportHelper.extractField(fd, "textField_mlgckllq");
            String date = getDateStr(fd, "dateField_mlelkrk3");
            sb.append("          <tr><td>").append(escHtml(submitter))
              .append("</td><td>").append(escHtml(title != null ? title : "-"))
              .append("</td><td>").append(escHtml(date)).append("</td></tr>\n");
        }

        sb.append("        </tbody>\n</table>\n")
          .append("    </div>\n");
        return sb.toString();
    }


    /**
     * 安全隐患台账：提交人 + 隐患等级(彩色tag) + 状态(彩色tag) + 日期
     */
    private String buildHazardContent(SourceSection src) {
        StringBuilder sb = new StringBuilder();
        sb.append("    <div class=\"section-content\">\n")
          .append("      <table class=\"data-table\">\n")
          .append("        <thead><tr><th>提交人</th><th>隐患等级</th><th>安全隐患状态</th><th>日期</th></tr></thead>\n")
          .append("        <tbody>\n");

        for (Map<String, ?> fd : src.getRecords()) {
            String submitter = getEmpName(fd, "employeeField_mpuvsdc4");
            String level = ReportHelper.extractField(fd, "radioField_mpuvsdbx");
            String status = ReportHelper.extractField(fd, "radioField_mpumsa4p");
            String date = getDateStr(fd, "dateField_mpuvsdbz");

            // 隐患等级彩色标签
            String lvBg = "danger_".equals(level2class(level)) ? "#fef2f2"
                        : "major_".equals(level2class(level)) ? "#fff7ed" : "#eef2ff";
            String lvColor = "danger_".equals(level2class(level)) ? "#dc2626"
                           : "major_".equals(level2class(level)) ? "#d97706" : "#4f46e5";

            // 状态彩色标签
            String stBg = "closed_".equals(status2class(status)) ? "#ecfdf5"
                        : "pending_".equals(status2class(status)) ? "#eef2ff" : "#fef3c7";
            String stColor = "closed_".equals(status2class(status)) ? "#059669"
                           : "pending_".equals(status2class(status)) ? "#4f46e5" : "#b45309";

            sb.append("          <tr>")
              .append("<td>").append(escHtml(submitter)).append("</td>")
              .append("<td><span class=\"tag\" style=\"background:").append(lvBg)
              .append(";color:").append(lvColor).append(";\">").append(escHtml(level)).append("</span></td>")
              .append("<td><span class=\"tag\" style=\"background:").append(stBg)
              .append(";color:").append(stColor).append(";\">").append(escHtml(status)).append("</span></td>")
              .append("<td>").append(escHtml(date)).append("</td>")
              .append("</tr>\n");
        }

        sb.append("        </tbody>\n</table>\n")
          .append("    </div>\n");
        return sb.toString();
    }


    /**
     * 简单数据源：提交人 + 日期
     * 用于 log, safeLog, station
     */
    private String buildSimpleContent(SourceSection src, String key) {
        String empField = getEmpField(key);
        String dateField = getDateField(key);

        StringBuilder sb = new StringBuilder();
        sb.append("    <div class=\"section-content\">\n")
          .append("      <table class=\"data-table\">\n")
          .append("        <thead><tr><th>提交人</th><th>日期</th></tr></thead>\n")
          .append("        <tbody>\n");

        for (Map<String, ?> fd : src.getRecords()) {
            String submitter = getEmpName(fd, empField);
            String date = getDateStr(fd, dateField);
            sb.append("          <tr><td>").append(escHtml(submitter))
              .append("</td><td>").append(escHtml(date)).append("</td></tr>\n");
        }

        sb.append("        </tbody>\n</table>\n")
          .append("    </div>\n");
        return sb.toString();
    }


    /**
     * 空数据源提示块。
     */
    private String buildEmptySection(String key) {
        String name = getSourceName(key);
        return ""
            + "<div class=\"source-section\">\n"
            + "  <details class=\"source-details\">\n"
            + "    <summary class=\"source-summary\">\n"
            + "      <span class=\"source-summary-left\">"
            + getSourceEmoji(key) + " " + name
            + " <span class=\"badge\">0 条</span></span>\n"
            + "      <span class=\"collapse-arrow\">\u25BC</span>\n"
            + "    </summary>\n"
            + "    <div class=\"section-content empty-hint\">该时间范围内暂无数据</div>\n"
            + "  </details>\n"
            + "</div>\n";
    }


    // ========================================
    //  字段映射（对应 xm.js F 对象）
    // ========================================

    private static String getEmpField(String key) {
        switch (key) {
            case "log":     return "employeeField_mjmamg0o";
            case "safeLog": return "employeeField_mjmamg0o";
            case "station": return "employeeField_mj89asvr";
            default:        return "";
        }
    }

    private static String getDateField(String key) {
        switch (key) {
            case "log":     return "dateField_mjjngk7x";
            case "safeLog": return "dateField_mjjngk7x";
            case "station": return "dateField_mjl61srf";
            default:        return "";
        }
    }

    private static String getSourceEmoji(String key) {
        switch (key) {
            case "docLib":  return "\uD83D\uDCDA";
            case "dynamic": return "\uD83D\uDCCA";
            case "log":     return "\uD83D\uDCCB";
            case "safeLog": return "\uD83D\uDEE1\uFE0F";
            case "station": return "\uD83D\uDD0D";
            case "hazard":  return "\u26A0\uFE0F";
            default:        return "\uD83D\uDCC4";
        }
    }

    private static String getSourceName(String key) {
        for (ReportConstants.SourceDef s : ReportConstants.SOURCES) {
            if (s.key.equals(key)) return s.label;
        }
        return key;
    }

    private static String level2class(String level) {
        if (level == null) return "";
        if (level.contains("重大")) return "danger_";
        if (level.contains("较大")) return "major_";
        return "";
    }

    private static String status2class(String status) {
        if (status == null) return "";
        if (status.contains("已整改")) return "closed_";
        if (status.contains("整改中")) return "pending_";
        return "";
    }


    // ========================================
    //  字段提取（对应 xm.js getEmpName / getDateStr）
    // ========================================

    @SuppressWarnings("unchecked")
    private static String getEmpName(Map<String, ?> fd, String fieldId) {
        if (fd == null || fieldId == null) return "-";
        Object val = fd.get(fieldId);
        if (val == null) return "-";

        if (val instanceof List) {
            List<Object> list = (List<Object>) val;
            if (list.isEmpty()) return "-";
            Object first = list.get(0);
            if (first instanceof Map) {
                Map<String, Object> m = (Map<String, Object>) first;
                Object label = m.get("label");
                if (label != null) return label.toString();
                Object name = m.get("name");
                if (name != null) return name.toString();
            }
            return first.toString();
        }

        if (val instanceof Map) {
            Map<String, Object> m = (Map<String, Object>) val;
            Object label = m.get("label");
            if (label != null) return label.toString();

            Object v = m.get("value");
            if (v instanceof List) {
                List<Object> list = (List<Object>) v;
                if (!list.isEmpty()) {
                    Object first = list.get(0);
                    if (first instanceof Map) {
                        Map<String, Object> fm = (Map<String, Object>) first;
                        Object fl = fm.get("label");
                        if (fl != null) return fl.toString();
                        Object fn = fm.get("name");
                        if (fn != null) return fn.toString();
                    }
                    return first.toString();
                }
            }
            if (v != null) return v.toString();
        }

        return val.toString();
    }

    private static String getDateStr(Map<String, ?> fd, String fieldId) {
        if (fd == null || fieldId == null) return "-";
        Object val = fd.get(fieldId);
        if (val == null) return "-";

        if (val instanceof Number) {
            return tsToDate(((Number) val).longValue());
        }

        if (val instanceof Map) {
            Map<String, Object> m = (Map<String, Object>) val;
            Object v = m.get("value");
            if (v instanceof Number) return tsToDate(((Number) v).longValue());
            if (v instanceof String) {
                String sv = (String) v;
                if (sv.matches("\\d+")) return tsToDate(Long.parseLong(sv));
                return sv;
            }
            if (v != null) return v.toString();
        }

        return val.toString();
    }

    private static String tsToDate(long ts) {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
        return sdf.format(new java.util.Date(ts));
    }


    // ========================================
    //  工具方法
    // ========================================

    private static String nowStr() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private static String escHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private static String buildFooter() {
        return "<div class=\"footer-note\">"
             + "本报告由系统自动生成 \uFF5C 报告生成时间：" + nowStr()
             + "</div>\n";
    }


    // ========================================
    //  CSS（xm.js 风格）
    // ========================================

    private static final String CSS = ""
        + "*, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }\n"
        + "body {\n"
        + "  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC',\n"
        + "    'Hiragino Sans GB', 'Microsoft YaHei', Helvetica, Arial, sans-serif;\n"
        + "  background: #f0f2f5;\n"
        + "  color: #1a1a2e;\n"
        + "  padding: 32px 0;\n"
        + "}\n"
        + ".container { max-width: 1200px; margin: 0 auto; padding: 0 24px; }\n"

        // Header
        + ".report-header {\n"
        + "  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);\n"
        + "  border-radius: 16px;\n"
        + "  padding: 28px 32px;\n"
        + "  color: #fff;\n"
        + "  margin-bottom: 24px;\n"
        + "  position: relative;\n"
        + "  overflow: hidden;\n"
        + "}\n"
        + ".report-header h1 {\n"
        + "  font-size: 18px;\n"
        + "  font-weight: 700;\n"
        + "  letter-spacing: 0.5px;\n"
        + "  margin-top: 4px;\n"
        + "}\n"
        + ".header-badge {\n"
        + "  display: inline-block;\n"
        + "  background: rgba(255,255,255,0.15);\n"
        + "  font-size: 11px;\n"
        + "  padding: 2px 12px;\n"
        + "  border-radius: 20px;\n"
        + "  font-weight: 500;\n"
        + "}\n"
        + ".subtitle {\n"
        + "  font-size: 13px;\n"
        + "  color: rgba(255,255,255,0.6);\n"
        + "  margin-top: 6px;\n"
        + "}\n"

        // 项目序号标题栏（xm.js 风格：1 项目名称）
        + ".project-title-bar {\n"
        + "  display: flex;\n"
        + "  align-items: center;\n"
        + "  gap: 10px;\n"
        + "  margin-bottom: 12px;\n"
        + "  margin-top: 20px;\n"
        + "}\n"
        + ".project-title-bar:first-of-type { margin-top: 0; }\n"
        + ".project-index {\n"
        + "  display: flex;\n"
        + "  align-items: center;\n"
        + "  justify-content: center;\n"
        + "  width: 28px;\n"
        + "  height: 28px;\n"
        + "  background: linear-gradient(135deg, #2563eb, #1d4ed8);\n"
        + "  color: #fff;\n"
        + "  font-size: 14px;\n"
        + "  font-weight: 700;\n"
        + "  border-radius: 8px;\n"
        + "  flex-shrink: 0;\n"
        + "}\n"
        + ".project-name-title {\n"
        + "  font-size: 18px;\n"
        + "  font-weight: 700;\n"
        + "  color: #1a1a2e;\n"
        + "}\n"

        // Section card
        + ".section-card {\n"
        + "  background: #fff;\n"
        + "  border-radius: 12px;\n"
        + "  padding: 20px 24px;\n"
        + "  margin-bottom: 16px;\n"
        + "  box-shadow: 0 1px 3px rgba(0,0,0,0.06);\n"
        + "}\n"
        + ".section-title {\n"
        + "  font-size: 16px;\n"
        + "  font-weight: 700;\n"
        + "  color: #1a1a2e;\n"
        + "  margin-bottom: 12px;\n"
        + "  padding-bottom: 8px;\n"
        + "  border-bottom: 2px solid #eef2ff;\n"
        + "}\n"

        // Badge
        + ".badge {\n"
        + "  display: inline-block;\n"
        + "  background: #eef2ff;\n"
        + "  color: #4f46e5;\n"
        + "  font-size: 11px;\n"
        + "  padding: 1px 8px;\n"
        + "  border-radius: 8px;\n"
        + "  font-weight: 500;\n"
        + "}\n"

        // Info grid
        + ".info-grid {\n"
        + "  display: grid;\n"
        + "  grid-template-columns: 1fr 1fr;\n"
        + "  gap: 6px 24px;\n"
        + "}\n"
        + ".info-item {\n"
        + "  display: flex;\n"
        + "  align-items: baseline;\n"
        + "  gap: 6px;\n"
        + "  padding: 6px 0;\n"
        + "  border-bottom: 1px dashed #f0f0f0;\n"
        + "}\n"
        + ".info-label {\n"
        + "  font-size: 13px;\n"
        + "  color: #8c8c8c;\n"
        + "  min-width: 70px;\n"
        + "  flex-shrink: 0;\n"
        + "}\n"
        + ".info-value {\n"
        + "  font-size: 14px;\n"
        + "  color: #1a1a2e;\n"
        + "  font-weight: 500;\n"
        + "}\n"
        + ".person-tags { display: flex; gap: 6px; flex-wrap: wrap; }\n"
        + ".person-tag {\n"
        + "  display: inline-block;\n"
        + "  font-size: 12px;\n"
        + "  padding: 2px 8px;\n"
        + "  border-radius: 4px;\n"
        + "  font-weight: 500;\n"
        + "}\n"
        + ".director-tag { background: #f5f3ff; color: #7c3aed; }\n"
        + ".engineer-tag { background: #f5f3ff; color: #7c3aed; }\n"

        // Stats grid
        + ".stats-grid {\n"
        + "  display: flex;\n"
        + "  gap: 10px;\n"
        + "  flex-wrap: wrap;\n"
        + "}\n"
        + ".stat-card {\n"
        + "  flex: 1 1 120px;\n"
        + "  text-align: center;\n"
        + "  padding: 16px 8px;\n"
        + "  background: #fafbfc;\n"
        + "  border-radius: 8px;\n"
        + "  border: 1px solid #f0f0f0;\n"
        + "  min-width: 100px;\n"
        + "}\n"
        + ".stat-label { font-size: 13px; color: #8c8c8c; margin-bottom: 8px; }\n"
        + ".stat-count { font-size: 22px; font-weight: 700; }\n"

        // Source section (collapsible)
        + ".source-section {\n"
        + "  background: #fff;\n"
        + "  border-radius: 10px;\n"
        + "  margin-bottom: 12px;\n"
        + "  box-shadow: 0 1px 3px rgba(0,0,0,0.06);\n"
        + "  overflow: hidden;\n"
        + "}\n"
        + ".source-details { display: block; }\n"
        + ".source-summary {\n"
        + "  display: flex;\n"
        + "  justify-content: space-between;\n"
        + "  align-items: center;\n"
        + "  padding: 14px 20px;\n"
        + "  cursor: pointer;\n"
        + "  user-select: none;\n"
        + "  border-bottom: 1px solid #f0f0f0;\n"
        + "}\n"
        + ".source-summary::-webkit-details-marker { display: none; }\n"
        + ".source-summary-left {\n"
        + "  font-weight: 700;\n"
        + "  font-size: 15px;\n"
        + "  color: #1a1a2e;\n"
        + "  display: flex;\n"
        + "  align-items: center;\n"
        + "  gap: 8px;\n"
        + "}\n"
        + ".collapse-arrow {\n"
        + "  font-size: 11px;\n"
        + "  color: #999;\n"
        + "  transition: transform 0.2s ease;\n"
        + "}\n"
        + ".source-details[open] .collapse-arrow {\n"
        + "  transform: rotate(0deg);\n"
        + "}\n"
        + ".source-details:not([open]) .collapse-arrow {\n"
        + "  transform: rotate(-90deg);\n"
        + "}\n"

        // Section content
        + ".section-content {\n"
        + "  padding: 12px 20px;\n"
        + "}\n"

        // Sub section (docLib groups)
        + ".sub-section {\n"
        + "  margin-bottom: 14px;\n"
        + "}\n"
        + ".sub-section:last-child { margin-bottom: 0; }\n"
        + ".sub-title {\n"
        + "  font-size: 14px;\n"
        + "  font-weight: 600;\n"
        + "  color: #4f46e5;\n"
        + "  margin-bottom: 8px;\n"
        + "  padding-bottom: 4px;\n"
        + "  border-bottom: 1px dashed #e8e8e8;\n"
        + "  display: flex;\n"
        + "  align-items: center;\n"
        + "  gap: 8px;\n"
        + "}\n"

        // Data table
        + ".data-table {\n"
        + "  width: 100%;\n"
        + "  border-collapse: collapse;\n"
        + "  font-size: 13px;\n"
        + "  border: 1px solid #f0f0f0;\n"
        + "  border-radius: 6px;\n"
        + "  overflow: hidden;\n"
        + "}\n"
        + ".data-table thead { background: #f8f9fb; }\n"
        + ".data-table th {\n"
        + "  padding: 8px 12px;\n"
        + "  text-align: left;\n"
        + "  font-weight: 600;\n"
        + "  color: #595959;\n"
        + "  font-size: 13px;\n"
        + "  border-bottom: 1px solid #f0f0f0;\n"
        + "}\n"
        + ".data-table td {\n"
        + "  padding: 7px 12px;\n"
        + "  border-bottom: 1px solid #f5f5f5;\n"
        + "  color: #262626;\n"
        + "}\n"
        + ".data-table tbody tr:last-child td { border-bottom: none; }\n"

        // Tag
        + ".tag {\n"
        + "  display: inline-block;\n"
        + "  font-size: 12px;\n"
        + "  padding: 2px 8px;\n"
        + "  border-radius: 4px;\n"
        + "  font-weight: 500;\n"
        + "  margin: 1px;\n"
        + "}\n"

        // Empty
        + ".empty-hint {\n"
        + "  text-align: center;\n"
        + "  padding: 24px;\n"
        + "  color: #8c8c8c;\n"
        + "  font-size: 14px;\n"
        + "}\n"

        // Footer
        + ".footer-note {\n"
        + "  text-align: center;\n"
        + "  font-size: 11px;\n"
        + "  color: #bfbfbf;\n"
        + "  padding: 20px 0 10px;\n"
        + "}\n"

        // Print
        + "@media print {\n"
        + "  body { background: #fff; padding: 0; }\n"
        + "  .report-header { border-radius: 0; }\n"
        + "  .source-summary { break-inside: avoid; }\n"
        + "  @page { margin: 15mm; }\n"
        + "}\n";
}
