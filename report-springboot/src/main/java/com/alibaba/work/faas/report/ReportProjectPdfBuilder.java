package com.alibaba.work.faas.report;

import com.alibaba.work.faas.report.model.ProjectReportData;
import com.alibaba.work.faas.report.model.ProjectReportData.PerProjectReport;
import com.alibaba.work.faas.report.model.ProjectReportData.SourceSection;
import com.alibaba.work.faas.report.model.ProjectReportData.ProjectBrief;

import com.alibaba.work.faas.report.ReportConstants.SourceDef;
import java.util.*;

/**
 * 项目报告 PDF 专用 XHTML 生成器。
 *
 * <p>和 {@link ReportProjectHtmlBuilder} 使用相同的 HTML 结构（div + class），
 * 仅 CSS 不同——使用 openhtmltopdf 兼容的 table/float 布局替代 flex/grid/gradient。
 * 这样 HTML 和 PDF 的页面结构完全一致。</p>
 *
 * @author Senior Developer
 * 创建于 2026/07/07
 */
public class ReportProjectPdfBuilder {

    public static final ReportProjectPdfBuilder INSTANCE = new ReportProjectPdfBuilder();
    private ReportProjectPdfBuilder() {}


    // ========================================
    //  入口
    // ========================================

    public String build(ProjectReportData data) {
        return build(data, null);
    }

    public String build(ProjectReportData data, Map<Integer, Integer> pageNumberMap) {
        return buildFull(data, pageNumberMap);
    }

    /**
     * 生成完整项目报告 HTML（封面 + 项目，单文档模式）。
     */
    public String buildFull(ProjectReportData data, Map<Integer, Integer> pageNumberMap) {
        StringBuilder html = new StringBuilder(16384);
        String title = data.isMultiProject()
                ? "全项目汇总报告"
                : "项目报告";
        html.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n")
            .append("<html xmlns=\"http://www.w3.org/1999/xhtml\" lang=\"zh-CN\">\n<head>\n")
            .append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"/>\n")
            .append("<title>").append(title).append("</title>\n")
            .append("<style type=\"text/css\">\n").append(PDF_CSS).append("</style>\n")
            .append("</head>\n<body>\n")
            .append("<div class=\"container\">\n")
            .append(buildCoverPage(data, pageNumberMap));

        for (int i = 0; i < data.getProjectReports().size(); i++) {
            PerProjectReport pr = data.getProjectReports().get(i);
            html.append("<div class=\"project-block\" id=\"project-")
                .append(i + 1).append("\">\n")
                .append(buildProjectBlock(pr, data, i + 1))
                .append("</div>\n");
        }

        html.append(buildFooter())
            .append("</div>\n</body>\n</html>");
        return html.toString();
    }

    /**
     * 生成仅包含封面和目录的 HTML（用于与项目正文 PDF 合并）。
     */
    public String buildCoverOnly(ProjectReportData data, Map<Integer, Integer> pageNumberMap) {
        StringBuilder html = new StringBuilder(8192);
        String title = data.isMultiProject()
                ? "全项目汇总报告"
                : "项目报告";
        html.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n")
            .append("<html xmlns=\"http://www.w3.org/1999/xhtml\" lang=\"zh-CN\">\n<head>\n")
            .append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"/>\n")
            .append("<title>").append(title).append(" - 封面</title>\n")
            .append("<style type=\"text/css\">\n").append(PDF_CSS).append("</style>\n")
            .append("</head>\n<body>\n")
            .append("<div class=\"container\">\n")
            .append(buildCoverPage(data, pageNumberMap))
            .append(buildFooter())
            .append("</div>\n</body>\n</html>");
        return html.toString();
    }

    /**
     * 生成仅包含项目正文的 HTML（用于与封面 PDF 合并，页码从 1 开始）。
     */
    public String buildProjectsOnly(ProjectReportData data, Map<Integer, Integer> pageNumberMap) {
        StringBuilder html = new StringBuilder(16384);
        String title = data.isMultiProject()
                ? "全项目汇总报告"
                : "项目报告";
        html.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n")
            .append("<html xmlns=\"http://www.w3.org/1999/xhtml\" lang=\"zh-CN\">\n<head>\n")
            .append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"/>\n")
            .append("<title>").append(title).append(" - 项目正文</title>\n")
            .append("<style type=\"text/css\">\n").append(PDF_CSS).append("</style>\n")
            .append("</head>\n<body>\n")
            .append("<div class=\"container\">\n")
            .append("<a id=\"toc\" style=\"height:0; font-size:0; line-height:0; display:block;\"></a>\n");

        for (int i = 0; i < data.getProjectReports().size(); i++) {
            PerProjectReport pr = data.getProjectReports().get(i);
            html.append("<div class=\"project-block\" id=\"project-")
                .append(i + 1).append("\">\n")
                .append(buildProjectBlock(pr, data, i + 1))
                .append("</div>\n");
        }

        html.append(buildFooter())
            .append("</div>\n</body>\n</html>");
        return html.toString();
    }


    // ========================================
    //  封面页（全项目模式：header + 目录）
    //  单项目模式：仅 header
    // ========================================

    private String buildCoverPage(ProjectReportData data) {
        return buildCoverPage(data, null);
    }

    private String buildCoverPage(ProjectReportData data, Map<Integer, Integer> pageNumberMap) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"cover-page\">\n")
          .append(buildHeader(data));

        if (data.isMultiProject() && data.getProjectCount() > 0) {
            sb.append(buildToc(data, pageNumberMap));
        }

        sb.append("</div>\n");
        return sb.toString();
    }


    // ========================================
    //  项目目录（带超链接）
    // ========================================

    private String buildToc(ProjectReportData data) {
        return buildToc(data, null);
    }

    private String buildToc(ProjectReportData data, Map<Integer, Integer> pageNumberMap) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"toc\" id=\"toc\">\n")
          .append("  <div class=\"toc-title\">项目目录</div>\n")
          .append("  <table class=\"toc-table\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\">\n")
          .append("    <thead><tr><th width=\"50\">序号</th><th>项目名称</th><th width=\"80\">数据量</th><th width=\"60\">页码</th></tr></thead>\n")
          .append("    <tbody>\n");

        List<PerProjectReport> projects = data.getProjectReports();
        for (int i = 0; i < projects.size(); i++) {
            PerProjectReport pr = projects.get(i);
            String name = escHtml(pr.getBrief().getName());
            int count = pr.getTotalRecords();
            String pageStr = (pageNumberMap != null && pageNumberMap.containsKey(i + 1))
                    ? String.valueOf(pageNumberMap.get(i + 1))
                    : "-";
            sb.append("      <tr>")
              .append("<td>").append(i + 1).append("</td>")
              .append("<td><a href=\"#project-").append(i + 1).append("\">").append(name).append("</a></td>")
              .append("<td>").append(count).append(" 条</td>")
              .append("<td>").append(pageStr).append("</td>")
              .append("</tr>\n");
        }

        sb.append("    </tbody>\n")
          .append("  </table>\n")
          .append("</div>\n");
        return sb.toString();
    }


    // ========================================
    //  头部（与 HTML Builder 完全相同的结构）
    // ========================================

    private String buildHeader(ProjectReportData data) {
        String badge = data.isMultiProject() ? "全项目汇总报告" : "项目报告";
        String subtitle = data.isMultiProject()
                ? "【全项目汇总报告-" + data.getTimeRangeLabel() + "】"
                : "【项目报告-" + data.getTimeRangeLabel() + "】";
        return ""
            + "<div class=\"report-header\">\n"
            + "  <div class=\"header-badge\">" + badge + "</div>\n"
            + "  <h1>" + subtitle + "</h1>\n"
            + "  <div class=\"subtitle\">报告生成时间：" + nowStr()
            + " | 统计范围：" + data.getDateStart() + " ~ " + data.getDateEnd()
            + (data.isMultiProject() ? " | 共 " + data.getProjectCount() + " 个项目" : "")
            + "</div>\n"
            + "</div>\n";
    }


    // ========================================
    //  项目区块
    // ========================================

    private String buildProjectBlock(PerProjectReport pr, ProjectReportData data, int index) {
        ProjectBrief brief = pr.getBrief();
        List<SourceSection> sources = pr.getSources();

        StringBuilder sb = new StringBuilder(4096);

        // 项目序号标题（可点击返回目录）
        // 嵌入不可见页码标记，供 PDF 文本提取精确识别项目起始页
        sb.append("<div class=\"project-title-bar\">\n")
          .append("  <a href=\"#toc\" class=\"project-title-link\">\n")
          .append("    <span class=\"page-marker\">__PROJECT_PAGE_").append(index).append("__</span>\n")
          .append("    <span class=\"project-index\">").append(index).append("</span>\n")
          .append("    <span class=\"project-name-title\">").append(escHtml(brief.getName())).append("</span>\n")
          .append("    <span class=\"badge\">").append(pr.getTotalRecords()).append(" 条</span>\n")
          .append("  </a>\n")
          .append("  <a href=\"#toc\" class=\"back-to-toc\">返回目录</a>\n")
          .append("</div>\n");

        // 项目信息卡片
        sb.append(buildProjectInfo(brief, data));

        // 统计卡片
        sb.append(buildStats(sources));

        // 6数据源明细
        sb.append(buildSourceSections(sources));

        return sb.toString();
    }


    // ========================================
    //  项目信息
    // ========================================

    private String buildProjectInfo(ProjectBrief brief, ProjectReportData data) {
        return ""
            + "<div class=\"section-card\">\n"
            + "  <div class=\"section-title\">项目信息</div>\n"
            + "  <div class=\"info-grid\">\n"
            + "    <table class=\"info-table\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\">\n"
            + "      <tr>\n"
            + "        <td class=\"info-label\">报告时间</td>\n"
            + "        <td class=\"info-value\">" + data.getDateStart() + " ~ " + data.getDateEnd() + "</td>\n"
            + "        <td class=\"info-label\">项目总监</td>\n"
            + "        <td class=\"info-value\">" + escHtml(brief.getDirector()) + "</td>\n"
            + "      </tr>\n"
            + "      <tr>\n"
            + "        <td class=\"info-label\">专监</td>\n"
            + "        <td class=\"info-value\">" + escHtml(brief.getEngineer()) + "</td>\n"
            + "        <td class=\"info-label\">项目地址</td>\n"
            + "        <td class=\"info-value\">" + escHtml(brief.getAddress()) + "</td>\n"
            + "      </tr>\n"
            + "      <tr>\n"
            + "        <td class=\"info-label\">人员</td>\n"
            + "        <td class=\"info-value\" colspan=\"3\">"
            + escHtml(brief.getDirector()) + "（总监） &nbsp; "
            + escHtml(brief.getEngineer()) + "（专监）</td>\n"
            + "      </tr>\n"
            + "    </table>\n"
            + "  </div>\n"
            + "</div>\n";
    }


    // ========================================
    //  统计卡片
    // ========================================

    private String buildStats(List<SourceSection> sources) {
        Map<String, Integer> countMap = new HashMap<>();
        for (SourceSection s : sources) {
            countMap.put(s.getKey(), s.getCount());
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"section-card\">\n")
          .append("  <div class=\"section-title\">各数据源统计</div>\n")
          .append("  <div class=\"stats-grid\">\n")

          // 使用 table 模拟 flex 网格
          .append("    <table class=\"stats-table\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\">\n")
          .append("      <tr>\n");

        for (SourceDef sd : ReportConstants.SOURCES) {
            int count = countMap.getOrDefault(sd.key, 0);
            sb.append("        <td class=\"stat-card\">\n")
              .append("          <div class=\"stat-label\">").append(sd.label).append("</div>\n")
              .append("          <div class=\"stat-count\" style=\"color:").append(sd.color).append(";\">")
              .append(count).append("</div>\n")
              .append("        </td>\n");
        }

        sb.append("      </tr>\n")
          .append("    </table>\n")
          .append("  </div>\n")
          .append("</div>\n");
        return sb.toString();
    }


    // ========================================
    //  6数据源明细（使用 div 结构，不用 details/summary）
    // ========================================

    private String buildSourceSections(List<SourceSection> sources) {
        Map<String, SourceSection> srcMap = new HashMap<>();
        for (SourceSection s : sources) {
            srcMap.put(s.getKey(), s);
        }

        StringBuilder sb = new StringBuilder();

        for (SourceDef sd : ReportConstants.SOURCES) {
            String key = sd.key;
            SourceSection src = srcMap.get(key);
            if (src == null || src.getRecords().isEmpty()) {
                sb.append(buildEmptySection(key));
                continue;
            }

            sb.append("<div class=\"source-section\">\n")
              .append("  <div class=\"source-header\">\n")
              .append("    <span class=\"source-header-left\">")
              .append(src.getLabel()).append(" <span class=\"badge\">").append(src.getCount()).append(" 条</span></span>\n")
              .append("  </div>\n")
              .append("  <div class=\"section-content\">\n");

            if ("docLib".equals(key)) {
                sb.append(buildDocLibContent(src));
            } else if ("hazard".equals(key)) {
                sb.append(buildHazardContent(src));
            } else if ("dynamic".equals(key)) {
                sb.append(buildDynamicContent(src));
            } else {
                sb.append(buildSimpleContent(src, key));
            }

            sb.append("  </div>\n")
              .append("</div>\n");
        }

        return sb.toString();
    }


    /**
     * 资料库：按分类分组。
     */
    private String buildDocLibContent(SourceSection src) {
        Map<String, List<Map<String, ?>>> groups = new LinkedHashMap<>();
        for (Map<String, ?> fd : src.getRecords()) {
            String cat = ReportHelper.extractCascadeLabel(fd, "cascadeSelectField_ml6no8w5");
            if (cat == null || cat.isEmpty()) cat = "未分类";
            groups.computeIfAbsent(cat, k -> new ArrayList<>()).add(fd);
        }

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, List<Map<String, ?>>> entry : groups.entrySet()) {
            String cat = entry.getKey();
            List<Map<String, ?>> items = entry.getValue();

            sb.append("    <div class=\"sub-section\">\n")
              .append("      <div class=\"sub-title\">").append(cat)
              .append(" <span class=\"badge\">").append(items.size()).append(" 条</span></div>\n")
              .append("      <table class=\"dt\" width=\"100%\" cellpadding=\"6\" cellspacing=\"0\">\n")
              .append("        <thead><tr><th>提交人</th><th>日期</th></tr></thead>\n")
              .append("        <tbody>\n");

            for (Map<String, ?> fd : items) {
                String submitter = getEmpName(fd, "employeeField_ml6no8vd");
                String date = getDateStr(fd, "dateField_ml6no8wb");
                sb.append("          <tr><td>").append(escHtml(submitter))
                  .append("</td><td>").append(escHtml(date)).append("</td></tr>\n");
            }

            sb.append("        </tbody>\n</table>\n    </div>\n");
        }

        return sb.toString();
    }


    /**
     * 项目动态：提交人 + 标题/内容 + 日期。
     */
    private String buildDynamicContent(SourceSection src) {
        StringBuilder sb = new StringBuilder();
        sb.append("    <table class=\"dt\" width=\"100%\" cellpadding=\"6\" cellspacing=\"0\">\n")
          .append("      <thead><tr><th>提交人</th><th>标题/内容</th><th>日期</th></tr></thead>\n")
          .append("      <tbody>\n");

        for (Map<String, ?> fd : src.getRecords()) {
            String submitter = getEmpName(fd, "employeeField_mlenlbgs");
            String title = ReportHelper.extractField(fd, "textField_mlgckllq");
            String date = getDateStr(fd, "dateField_mlelkrk3");
            sb.append("        <tr><td>").append(escHtml(submitter))
              .append("</td><td>").append(escHtml(title != null ? title : "-"))
              .append("</td><td>").append(escHtml(date)).append("</td></tr>\n");
        }

        sb.append("      </tbody>\n</table>\n");
        return sb.toString();
    }


    /**
     * 安全隐患台账：提交人 + 隐患等级(彩色tag) + 状态(彩色tag) + 日期。
     */
    private String buildHazardContent(SourceSection src) {
        StringBuilder sb = new StringBuilder();
        sb.append("    <table class=\"dt\" width=\"100%\" cellpadding=\"6\" cellspacing=\"0\">\n")
          .append("      <thead><tr><th>提交人</th><th>隐患等级</th><th>安全隐患状态</th><th>日期</th></tr></thead>\n")
          .append("      <tbody>\n");

        for (Map<String, ?> fd : src.getRecords()) {
            String submitter = getEmpName(fd, "employeeField_mpuvsdc4");
            String level = ReportHelper.extractField(fd, "radioField_mpuvsdbx");
            String status = ReportHelper.extractField(fd, "radioField_mpumsa4p");
            String date = getDateStr(fd, "dateField_mpuvsdbz");

            String lvBg = "danger_".equals(level2class(level)) ? "#fef2f2"
                        : "major_".equals(level2class(level)) ? "#fff7ed" : "#eef2ff";
            String lvColor = "danger_".equals(level2class(level)) ? "#dc2626"
                           : "major_".equals(level2class(level)) ? "#d97706" : "#4f46e5";
            String stBg = "closed_".equals(status2class(status)) ? "#ecfdf5"
                        : "pending_".equals(status2class(status)) ? "#eef2ff" : "#fef3c7";
            String stColor = "closed_".equals(status2class(status)) ? "#059669"
                           : "pending_".equals(status2class(status)) ? "#4f46e5" : "#b45309";

            sb.append("        <tr>")
              .append("<td>").append(escHtml(submitter)).append("</td>")
              .append("<td><span class=\"pdtag\" style=\"background:").append(lvBg)
              .append(";color:").append(lvColor).append(";\">").append(escHtml(level)).append("</span></td>")
              .append("<td><span class=\"pdtag\" style=\"background:").append(stBg)
              .append(";color:").append(stColor).append(";\">").append(escHtml(status)).append("</span></td>")
              .append("<td>").append(escHtml(date)).append("</td>")
              .append("</tr>\n");
        }

        sb.append("      </tbody>\n</table>\n");
        return sb.toString();
    }


    /**
     * 简单数据源：提交人 + 日期。
     */
    private String buildSimpleContent(SourceSection src, String key) {
        String empField = getEmpField(key);
        String dateField = getDateField(key);

        StringBuilder sb = new StringBuilder();
        sb.append("    <table class=\"dt\" width=\"100%\" cellpadding=\"6\" cellspacing=\"0\">\n")
          .append("      <thead><tr><th>提交人</th><th>日期</th></tr></thead>\n")
          .append("      <tbody>\n");

        for (Map<String, ?> fd : src.getRecords()) {
            String submitter = getEmpName(fd, empField);
            String date = getDateStr(fd, dateField);
            sb.append("        <tr><td>").append(escHtml(submitter))
              .append("</td><td>").append(escHtml(date)).append("</td></tr>\n");
        }

        sb.append("      </tbody>\n</table>\n");
        return sb.toString();
    }


    /**
     * 空数据源提示。（与 HTML Builder 结构相同，用 section-content + empty-hint）
     */
    private String buildEmptySection(String key) {
        String name = getSourceName(key);
        return ""
            + "<div class=\"source-section\">\n"
            + "  <div class=\"source-header\">\n"
            + "    <span class=\"source-header-left\">" + name
            + " <span class=\"badge\">0 条</span></span>\n"
            + "  </div>\n"
            + "  <div class=\"section-content empty-hint\">该时间范围内暂无数据</div>\n"
            + "</div>\n";
    }


    // ========================================
    //  字段映射
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
    //  字段提取
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
        if (val instanceof Number) return tsToDate(((Number) val).longValue());
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
        return ReportTemplateUtil.nowStr();
    }

    private static String escHtml(String s) {
        return ReportTemplateUtil.escHtml(s);
    }

    private static String buildFooter() {
        return ReportTemplateUtil.buildFooter();
    }


    // ========================================
    //  CSS —— 与 HTML Builder 结构一致，但仅用 openhtmltopdf 兼容的属性
    //  使用 table 布局替代 flex/grid，实色替代渐变
    // ========================================

    private static final String PDF_CSS = ""
        + "*, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }\n"
        + "body {\n"
        + "  font-family: 'Microsoft YaHei', 'SimHei', 'PingFang SC', sans-serif;\n"
        + "  font-size: 12px;\n"
        + "  color: #333;\n"
        + "  margin: 0; padding: 0;\n"
        + "  background: #f0f2f5;\n"
        + "}\n"
        + ".container { max-width: 1200px; margin: 0 auto; padding: 0 24px; }\n"

        // 封面页：与正文强制分页
        + ".cover-page { page-break-after: always; }\n"

        // 项目目录
        + ".toc { margin-top: 24px; }\n"
        + ".toc-title {\n"
        + "  font-size: 16px; font-weight: 700;\n"
        + "  color: #1a1a2e;\n"
        + "  margin-bottom: 12px;\n"
        + "  padding-bottom: 6px;\n"
        + "  border-bottom: 2px solid #eef2ff;\n"
        + "}\n"
        + ".toc-table { border-collapse: collapse; font-size: 12px; }\n"
        + ".toc-table thead th {\n"
        + "  padding: 8px 10px;\n"
        + "  text-align: left;\n"
        + "  background: #f8f9fb;\n"
        + "  border-bottom: 1px solid #e8e8e8;\n"
        + "  color: #595959;\n"
        + "  font-weight: 600;\n"
        + "}\n"
        + ".toc-table tbody td {\n"
        + "  padding: 8px 10px;\n"
        + "  border-bottom: 1px dashed #e8e8e8;\n"
        + "  vertical-align: middle;\n"
        + "}\n"
        + ".toc-table a {\n"
        + "  color: #2563eb;\n"
        + "  text-decoration: none;\n"
        + "  font-weight: 500;\n"
        + "}\n"
        + ".toc-table a:hover { text-decoration: underline; }\n"

        // 项目区块：每个项目从新页开始
        + ".project-block { page-break-before: always; }\n"

        // Header（深色底色替代渐变）
        + ".report-header {\n"
        + "  background-color: #1a1a2e;\n"
        + "  padding: 20px 24px;\n"
        + "  color: #fff;\n"
        + "  margin-bottom: 0;\n"
        + "  border-radius: 14px;\n"
        + "}\n"
        + ".report-header h1 {\n"
        + "  font-size: 16px;\n"
        + "  font-weight: 700;\n"
        + "  margin-top: 4px;\n"
        + "}\n"
        + ".header-badge {\n"
        + "  display: inline-block;\n"
        + "  background-color: #333366;\n"
        + "  font-size: 11px;\n"
        + "  padding: 2px 12px;\n"
        + "  font-weight: 500;\n"
        + "  border-radius: 10px;\n"
        + "}\n"
        + ".subtitle {\n"
        + "  font-size: 12px;\n"
        + "  color: #888899;\n"
        + "  margin-top: 6px;\n"
        + "}\n"

        // 项目序号标题栏（含返回目录链接）
        + ".project-title-bar { margin-bottom: 8px; margin-top: 6px; "
        + "display: flex; align-items: center; flex-wrap: wrap; }\n"
        + ".page-marker {\n"
        + "  color: transparent;\n"
        + "  font-size: 0.1px;\n"
        + "  line-height: 0;\n"
        + "  vertical-align: middle;\n"
        + "}\n"
        + ".project-title-link { text-decoration: none; color: inherit; display: inline-flex; align-items: center; }\n"
        + ".project-index {\n"
        + "  display: inline-block;\n"
        + "  width: 24px; height: 24px; line-height: 24px;\n"
        + "  background-color: #2563eb;\n"
        + "  color: #fff;\n"
        + "  font-size: 12px; font-weight: 700;\n"
        + "  text-align: center;\n"
        + "  vertical-align: middle;\n"
        + "  border-radius: 6px;\n"
        + "}\n"
        + ".project-name-title {\n"
        + "  font-size: 15px;\n"
        + "  font-weight: 700;\n"
        + "  color: #1a1a2e;\n"
        + "  padding-left: 6px;\n"
        + "}\n"
        + ".back-to-toc {\n"
        + "  font-size: 10px;\n"
        + "  color: #2563eb;\n"
        + "  text-decoration: none;\n"
        + "  margin-left: 8px;\n"
        + "  padding: 1px 6px;\n"
        + "  border: 1px solid #2563eb;\n"
        + "  border-radius: 4px;\n"
        + "  white-space: nowrap;\n"
        + "}\n"

        // Section card（白卡片）
        + ".section-card {\n"
        + "  background: #fff;\n"
        + "  margin-bottom: 10px;\n"
        + "  padding: 12px 16px;\n"
        + "  border: 1px solid #e8e8e8;\n"
        + "  border-radius: 10px;\n"
        + "}\n"
        + ".section-title {\n"
        + "  font-size: 14px;\n"
        + "  font-weight: 700;\n"
        + "  color: #1a1a2e;\n"
        + "  margin-bottom: 8px;\n"
        + "  padding-bottom: 4px;\n"
        + "  border-bottom: 2px solid #eef2ff;\n"
        + "}\n"

        // Badge
        + ".badge {\n"
        + "  display: inline-block;\n"
        + "  background: #eef2ff;\n"
        + "  color: #4f46e5;\n"
        + "  font-size: 10px;\n"
        + "  padding: 1px 7px;\n"
        + "  font-weight: 500;\n"
        + "  border-radius: 7px;\n"
        + "}\n"

        // 信息表格
        + ".info-table { border-collapse: collapse; }\n"
        + ".info-label {\n"
        + "  font-size: 11px; color: #8c8c8c;\n"
        + "  width: 70px;\n"
        + "  vertical-align: top;\n"
        + "  padding: 4px 8px 4px 0;\n"
        + "  border-bottom: 1px dashed #f0f0f0;\n"
        + "}\n"
        + ".info-value {\n"
        + "  font-size: 12px; color: #1a1a2e; font-weight: 500;\n"
        + "  padding: 4px 16px 4px 0;\n"
        + "  border-bottom: 1px dashed #f0f0f0;\n"
        + "}\n"

        // 统计卡片网格
        + ".stats-grid { margin-bottom: 4px; }\n"
        + ".stats-table { border-collapse: separate; border-spacing: 6px; }\n"
        + ".stat-card {\n"
        + "  background: #fafbfc;\n"
        + "  border: 1px solid #f0f0f0;\n"
        + "  text-align: center;\n"
        + "  padding: 10px 4px;\n"
        + "  width: 16%;\n"
        + "}\n"
        + ".stat-label { font-size: 11px; color: #8c8c8c; margin-bottom: 4px; }\n"
        + ".stat-count { font-size: 18px; font-weight: 700; }\n"

        // 数据源区块（允许自然分页，避免空白）
        + ".source-section {\n"
        + "  background: #fff;\n"
        + "  margin-bottom: 8px;\n"
        + "  border: 1px solid #e8e8e8;\n"
        + "  border-radius: 10px;\n"
        + "}\n"
        + ".source-header {\n"
        + "  padding: 8px 12px;\n"
        + "  border-bottom: 1px solid #f0f0f0;\n"
        + "}\n"
        + ".source-header-left {\n"
        + "  font-weight: 700;\n"
        + "  font-size: 13px;\n"
        + "  color: #1a1a2e;\n"
        + "}\n"
        + ".section-content {\n"
        + "  padding: 8px 12px;\n"
        + "}\n"

        // 资料库子分类
        + ".sub-section {\n"
        + "  margin-bottom: 8px;\n"
        + "}\n"
        + ".sub-section:last-child { margin-bottom: 0; }\n"
        + ".sub-title {\n"
        + "  font-size: 12px;\n"
        + "  font-weight: 600;\n"
        + "  color: #4f46e5;\n"
        + "  margin-bottom: 4px;\n"
        + "  padding-bottom: 2px;\n"
        + "  border-bottom: 1px dashed #e8e8e8;\n"
        + "}\n"

        // 数据表格
        + ".dt {\n"
        + "  width: 100%;\n"
        + "  border-collapse: collapse;\n"
        + "  font-size: 11px;\n"
        + "  border: 1px solid #f0f0f0;\n"
        + "}\n"
        + ".dt thead { background: #f8f9fb; }\n"
        + ".dt thead th {\n"
        + "  padding: 5px 8px;\n"
        + "  text-align: left;\n"
        + "  font-weight: 600;\n"
        + "  color: #595959;\n"
        + "  font-size: 10px;\n"
        + "  border-bottom: 1px solid #e8e8e8;\n"
        + "  border-right: 1px solid #f0f0f0;\n"
        + "}\n"
        + ".dt thead th:last-child { border-right: none; }\n"
        + ".dt tbody td {\n"
        + "  padding: 4px 8px;\n"
        + "  border-bottom: 1px solid #eee;\n"
        + "  border-right: 1px solid #f5f5f5;\n"
        + "  color: #333;\n"
        + "  vertical-align: middle;\n"
        + "}\n"
        + ".dt tbody td:last-child { border-right: none; }\n"
        + ".dt tbody tr:last-child td { border-bottom: none; }\n"

        // PDF tag
        + ".pdtag {\n"
        + "  display: inline-block;\n"
        + "  font-size: 10px;\n"
        + "  padding: 1px 6px;\n"
        + "  font-weight: 500;\n"
        + "  border-radius: 3px;\n"
        + "}\n"

        // Empty
        + ".empty-hint {\n"
        + "  text-align: center;\n"
        + "  padding: 20px;\n"
        + "  color: #8c8c8c;\n"
        + "  font-size: 13px;\n"
        + "}\n"

        // 页脚
        + ".footer-note {\n"
        + "  text-align: center;\n"
        + "  font-size: 10px;\n"
        + "  color: #bfbfbf;\n"
        + "  padding: 16px 0 8px;\n"
        + "}\n"

        // 打印（无自动页码，改为 PDFBox 后处理注入）
        + "@page {\n"
        + "  margin: 12mm 15mm 18mm 15mm;\n"
        + "}\n";
}
