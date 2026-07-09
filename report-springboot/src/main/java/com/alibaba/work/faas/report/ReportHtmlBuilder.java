package com.alibaba.work.faas.report;

import java.util.List;
import java.util.Map;

/**
 * 报表 HTML 生成器。
 *
 * <p>接收 {@link ReportData}，生成与 report-platform-preview.html 样式一致的 HTML 字符串。
 * 样式直接内嵌在 HTML 中，不做任何修改。</p>
 *
 * @author Senior Developer
 * 创建于 2026/07/06
 */
public class ReportHtmlBuilder {

    // 单例，保持与 ReportService 一致
    public static final ReportHtmlBuilder INSTANCE = new ReportHtmlBuilder();

    private ReportHtmlBuilder() {}


    /**
     * 构建完整报表 HTML（XHTML 合规，可直接用于 Flying Saucer PDF 渲染）。
     */
    public String build(ReportData data) {
        StringBuilder html = new StringBuilder(8192);
        html.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n")
            .append("<html xmlns=\"http://www.w3.org/1999/xhtml\" lang=\"zh-CN\">\n<head>\n")
            .append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"/>\n")
            .append("<title>平台运营报告</title>\n")
            .append("<style type=\"text/css\">\n")
            .append(CSS)
            .append("</style>\n</head>\n<body>\n")
            .append("<div class=\"container\">\n")
            .append(buildHeader(data))
            .append(buildSection1(data))
            .append(buildSection2(data))
            .append(buildSection3(data))
            .append(buildFooter())
            .append("</div>\n</body>\n</html>");
        return html.toString();
    }

    // ========================================
    //  Header
    // ========================================

    private String buildHeader(ReportData data) {
        return ""
            + "<header class=\"report-header\">\n"
            + "  <div class=\"report-header-top\">\n"
            + "    <div class=\"report-title\">\n"
            + "      <div class=\"report-type-badge\">📊 平台运营报告</div>\n"
            + "      <h1>【平台运营报告-" + (data.getRangeKey().contains("week") ? "周报" : "月报") + "-" + data.getTimeRangeLabel() + "】</h1>\n"
            + "      <div class=\"subtitle\">报告生成时间：" + nowStr() + "</div>\n"
            + "    </div>\n"
            + "  </div>\n"
            + "</header>\n";
    }

    // ========================================
    //  Section 1: 平台基本运营数据
    // ========================================

    private String buildSection1(ReportData data) {
        return ""
            + "<div class=\"section-card\">\n"
            + "  <div class=\"section-title\"><span class=\"num\">1</span>平台基本运营数据</div>\n"
            + "  <div class=\"summary-grid\">\n"
            + "    <div class=\"summary-item\">\n"
            + "      <div class=\"label\">📁 报告时间</div>\n"
            + "      <div class=\"value blue\" style=\"font-size:18px;\">" + data.getTimeRangeLabel() + "</div>\n"
            + "    </div>\n"
            + "    <div class=\"summary-item\">\n"
            + "      <div class=\"label\">🏗️ 项目数量</div>\n"
            + "      <div class=\"value green\">" + data.getProjects().size()
            +         " <span style=\"font-size:14px;font-weight:400;color:#8c8c8c;\">个</span></div>\n"
            + "    </div>\n"
            + "    <div class=\"summary-item\">\n"
            + "      <div class=\"label\">📝 " + data.getPeriodName() + "总记录</div>\n"
            + "      <div class=\"value orange\">" + data.getTotalRecords()
            +         " <span style=\"font-size:14px;font-weight:400;color:#8c8c8c;\">条</span></div>\n"
            + "    </div>\n"
            + "  </div>\n"
            + "</div>\n";
    }

    // ========================================
    //  Section 2: 项目基本信息
    // ========================================

    private String buildSection2(ReportData data) {
        if (data.getProjects().isEmpty()) {
            return ""
                + "<div class=\"section-card\">\n"
                + "  <div class=\"section-title\"><span class=\"num\">2</span>项目基本信息</div>\n"
                + "  <div style=\"padding:20px;color:#8c8c8c;text-align:center;\">暂无项目数据</div>\n"
                + "</div>\n";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"section-card\">\n")
          .append("  <div class=\"section-title\"><span class=\"num\">2</span>项目基本信息</div>\n")
          .append("  <div id=\"projectInfoTable\">\n")
          .append("    <table class=\"summary-count-table\">\n")
          .append("      <thead><tr>\n")
          .append("        <th style=\"text-align:left;width:18%;\">项目名称</th>\n")
          .append("        <th>负责人</th><th>项目总监</th><th>区域</th>")
          .append("<th>").append(data.getPeriodName()).append("总记录</th>\n")
          .append("      </tr></thead>\n")
          .append("      <tbody>\n");

        int idx = 0;
        for (ReportData.ProjectStat p : data.getProjects()) {
            sb.append("        <tr>\n")
              .append("          <td style=\"text-align:left;\">")
              .append(escHtml(p.getName())).append("</td>\n")
              .append("          <td><span class=\"role-tag person\">").append(escHtml(p.getDirector())).append("</span></td>\n")
              .append("          <td><span class=\"role-tag director\">").append(escHtml(p.getDirector())).append("</span></td>\n")
              .append("          <td><span class=\"area-badge\">").append(escHtml(p.getArea())).append("</span></td>\n")
              .append("          <td><strong style=\"color:#3b82f6;font-size:14px;\">")
              .append(p.getTotalCount()).append("</strong></td>\n")
              .append("        </tr>\n");
        }

        sb.append("      </tbody>\n")
          .append("    </table>\n")
          .append("  </div>\n")
          .append("</div>\n");
        return sb.toString();
    }

    // ========================================
    //  Section 3: 项目清单 · 数据统计
    // ========================================

    private String buildSection3(ReportData data) {
        if (data.getProjects().isEmpty()) {
            return ""
                + "<div class=\"section-card\">\n"
                + "  <div class=\"section-title\"><span class=\"num\">3</span>项目清单 · 数据统计</div>\n"
                + "  <div style=\"padding:20px;color:#8c8c8c;text-align:center;\">暂无项目数据</div>\n"
                + "</div>\n";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"section-card\">\n")
          .append("  <div class=\"section-title\"><span class=\"num\">3</span>项目清单 · 数据统计</div>\n");

        int projIdx = 1;
        for (ReportData.ProjectStat p : data.getProjects()) {
            sb.append("  <div class=\"project-block\">\n")
              .append("    <div class=\"project-block-header\">\n")
              .append("      <div class=\"left\">\n")
              .append("        <span class=\"p-index\">").append(projIdx).append("</span>\n")
              .append("        <span class=\"p-name\">").append(escHtml(p.getName())).append("</span>\n")
              .append("      </div>\n")
              .append("      <div class=\"p-stats\">").append(data.getPeriodName())
              .append(" <strong>").append(p.getTotalCount()).append("</strong> 条 ｜ ")
              .append(escHtml(p.getArea())).append("</div>\n")
              .append("    </div>\n")
              .append("    <div class=\"project-block-content open\" style=\"display:block;\">\n")
              .append("      <div class=\"source-mini-row\" style=\"grid-template-columns:repeat(6,1fr);\">\n");

            // 6 个数据源卡片
            for (int si = 0; si < ReportConstants.SOURCES.size(); si++) {
                ReportConstants.SourceDef src = ReportConstants.SOURCES.get(si);
                Integer count = p.getSourceCounts().getOrDefault(src.key, 0);
                sb.append("        <div class=\"source-mini-card\">\n")
                  .append("          <div class=\"s-name\" style=\"font-size:12px;\">").append(src.label).append("</div>\n")
                  .append("          <div class=\"s-count\" style=\"color:").append(src.color).append(";\">")
                  .append(count).append("</div>\n")
                  .append("          <div class=\"s-label\">").append(data.getPeriodName()).append("</div>\n")
                  .append("        </div>\n");
            }

            sb.append("      </div>\n")
              .append("    </div>\n")
              .append("  </div>\n");
            projIdx++;
        }

        sb.append("</div>\n");
        return sb.toString();
    }

    // ========================================
    //  Footer
    // ========================================

    private String buildFooter() {
        return "<div class=\"footer-note\">"
             + "本报告由系统自动生成 ｜ 报告生成时间：" + nowStr()
             + "</div>\n";
    }


    // ========================================
    //  工具方法
    // ========================================

    private static String nowStr() {
        return ReportTemplateUtil.nowStr();
    }

    /** HTML 转义 */
    private static String escHtml(String s) {
        return ReportTemplateUtil.escHtml(s);
    }


    // ========================================
    //  CSS（直接从 report-platform-preview.html 复制，不做任何修改）
    // ========================================

    private static final String CSS = ""
        + "*, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }\n"
        + "body {\n"
        + "  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Hiragino Sans GB',\n"
        + "    'Microsoft YaHei', 'Helvetica Neue', Helvetica, Arial, sans-serif;\n"
        + "  background: #f0f2f5;\n"
        + "  color: #1a1a2e;\n"
        + "  padding: 32px 0;\n"
        + "}\n"
        + ".container { max-width: 1200px; margin: 0 auto; padding: 0 24px; }\n"
        + ".report-header {\n"
        + "  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);\n"
        + "  border-radius: 16px;\n"
        + "  padding: 32px 36px;\n"
        + "  color: #fff;\n"
        + "  margin-bottom: 24px;\n"
        + "  position: relative;\n"
        + "  overflow: hidden;\n"
        + "}\n"
        + ".report-header::after {\n"
        + "  content: '';\n"
        + "  position: absolute;\n"
        + "  top: -60%;\n"
        + "  right: -8%;\n"
        + "  width: 380px; height: 380px;\n"
        + "  background: radial-gradient(circle, rgba(83,127,231,0.12) 0%, transparent 70%);\n"
        + "  border-radius: 50%;\n"
        + "}\n"
        + ".report-header-top {\n"
        + "  display: flex;\n"
        + "  justify-content: space-between;\n"
        + "  align-items: flex-start;\n"
        + "  position: relative; z-index: 1;\n"
        + "}\n"
        + ".report-title h1 {\n"
        + "  font-size: 20px;\n"
        + "  font-weight: 700;\n"
        + "  letter-spacing: 0.5px;\n"
        + "}\n"
        + ".report-type-badge {\n"
        + "  display: inline-block;\n"
        + "  background: rgba(255,255,255,0.15);\n"
        + "  font-size: 11px;\n"
        + "  padding: 2px 12px;\n"
        + "  border-radius: 20px;\n"
        + "  font-weight: 500;\n"
        + "  margin-bottom: 8px;\n"
        + "}\n"
        + ".subtitle {\n"
        + "  font-size: 13px;\n"
        + "  color: rgba(255,255,255,0.6);\n"
        + "  margin-top: 4px;\n"
        + "}\n"
        + ".section-title {\n"
        + "  display: flex;\n"
        + "  align-items: center;\n"
        + "  gap: 10px;\n"
        + "  font-size: 17px;\n"
        + "  font-weight: 600;\n"
        + "  color: #1a1a2e;\n"
        + "  margin-bottom: 16px;\n"
        + "  padding-bottom: 10px;\n"
        + "  border-bottom: 2px solid #eef2ff;\n"
        + "}\n"
        + ".section-title .num {\n"
        + "  display: inline-flex;\n"
        + "  width: 28px; height: 28px;\n"
        + "  background: linear-gradient(135deg, #3b82f6, #2563eb);\n"
        + "  color: #fff;\n"
        + "  border-radius: 8px;\n"
        + "  align-items: center;\n"
        + "  justify-content: center;\n"
        + "  font-size: 13px;\n"
        + "  font-weight: 700;\n"
        + "  flex-shrink: 0;\n"
        + "}\n"
        + ".section-card {\n"
        + "  background: #fff;\n"
        + "  border-radius: 12px;\n"
        + "  padding: 24px 28px;\n"
        + "  margin-bottom: 24px;\n"
        + "  box-shadow: 0 1px 3px rgba(0,0,0,0.06);\n"
        + "}\n"
        + ".summary-grid {\n"
        + "  display: grid;\n"
        + "  grid-template-columns: repeat(3, 1fr);\n"
        + "  gap: 16px;\n"
        + "}\n"
        + ".summary-item {\n"
        + "  text-align: center;\n"
        + "  padding: 20px;\n"
        + "  background: #fafbfc;\n"
        + "  border-radius: 10px;\n"
        + "  border: 1px solid #f0f0f0;\n"
        + "}\n"
        + ".summary-item .label {\n"
        + "  font-size: 13px;\n"
        + "  color: #8c8c8c;\n"
        + "  margin-bottom: 8px;\n"
        + "}\n"
        + ".summary-item .value {\n"
        + "  font-size: 28px;\n"
        + "  font-weight: 700;\n"
        + "}\n"
        + ".summary-item .value.blue { color: #3b82f6; }\n"
        + ".summary-item .value.green { color: #10b981; }\n"
        + ".summary-item .value.orange { color: #f59e0b; }\n"
        + ".summary-count-table {\n"
        + "  width: 100%;\n"
        + "  border-collapse: collapse;\n"
        + "  font-size: 13px;\n"
        + "  border-radius: 8px;\n"
        + "  overflow: hidden;\n"
        + "  border: 1px solid #f0f0f0;\n"
        + "}\n"
        + ".summary-count-table thead { background: #f8f9fb; }\n"
        + ".summary-count-table th {\n"
        + "  padding: 10px 14px;\n"
        + "  text-align: center;\n"
        + "  font-weight: 600;\n"
        + "  color: #595959;\n"
        + "  font-size: 12px;\n"
        + "  border-bottom: 1px solid #f0f0f0;\n"
        + "}\n"
        + ".summary-count-table td {\n"
        + "  padding: 10px 14px;\n"
        + "  text-align: center;\n"
        + "  border-bottom: 1px solid #f5f5f5;\n"
        + "  color: #262626;\n"
        + "}\n"
        + ".role-tag {\n"
        + "  display: inline-block;\n"
        + "  font-size: 11px;\n"
        + "  padding: 2px 8px;\n"
        + "  border-radius: 4px;\n"
        + "  font-weight: 400;\n"
        + "}\n"
        + ".role-tag.person { background: #ecfdf5; color: #059669; }\n"
        + ".role-tag.director { background: #fff7ed; color: #d97706; }\n"
        + ".area-badge {\n"
        + "  display: inline-block;\n"
        + "  background: #eef2ff;\n"
        + "  color: #4f46e5;\n"
        + "  font-size: 11px;\n"
        + "  padding: 2px 10px;\n"
        + "  border-radius: 4px;\n"
        + "  font-weight: 500;\n"
        + "}\n"
        + ".project-block {\n"
        + "  background: #fafbfc;\n"
        + "  border: 1px solid #f0f0f0;\n"
        + "  border-radius: 10px;\n"
        + "  margin-bottom: 16px;\n"
        + "  overflow: hidden;\n"
        + "}\n"
        + ".project-block:last-child { margin-bottom: 0; }\n"
        + ".project-block-header {\n"
        + "  display: flex;\n"
        + "  justify-content: space-between;\n"
        + "  align-items: center;\n"
        + "  padding: 14px 20px;\n"
        + "  background: #fff;\n"
        + "  border-bottom: 1px solid #f0f0f0;\n"
        + "}\n"
        + ".project-block-header .left {\n"
        + "  display: flex;\n"
        + "  align-items: center;\n"
        + "  gap: 10px;\n"
        + "}\n"
        + ".project-block-header .p-index {\n"
        + "  width: 28px; height: 28px;\n"
        + "  background: linear-gradient(135deg, #3b82f6, #2563eb);\n"
        + "  color: #fff;\n"
        + "  border-radius: 6px;\n"
        + "  display: flex;\n"
        + "  align-items: center;\n"
        + "  justify-content: center;\n"
        + "  font-size: 12px;\n"
        + "  font-weight: 700;\n"
        + "  flex-shrink: 0;\n"
        + "}\n"
        + ".project-block-header .p-name {\n"
        + "  font-size: 14px;\n"
        + "  font-weight: 600;\n"
        + "}\n"
        + ".project-block-header .p-stats {\n"
        + "  font-size: 12px;\n"
        + "  color: #8c8c8c;\n"
        + "}\n"
        + ".project-block-header .p-stats strong { color: #1a1a2e; }\n"
        + ".project-block-content { padding: 16px 20px; display: block; }\n"
        + ".source-mini-row {\n"
        + "  display: grid;\n"
        + "  gap: 10px;\n"
        + "  margin-bottom: 0;\n"
        + "}\n"
        + ".source-mini-card {\n"
        + "  background: #fff;\n"
        + "  border-radius: 8px;\n"
        + "  padding: 14px 8px;\n"
        + "  text-align: center;\n"
        + "  border: 1px solid #f0f0f0;\n"
        + "}\n"
        + ".source-mini-card .s-name { font-size: 12px; font-weight: 500; color: #1a1a2e; margin-bottom: 6px; }\n"
        + ".source-mini-card .s-count { font-size: 15px; font-weight: 700; }\n"
        + ".source-mini-card .s-label { font-size: 10px; color: #8c8c8c; }\n"
        + ".footer-note {\n"
        + "  text-align: center;\n"
        + "  font-size: 11px;\n"
        + "  color: #bfbfbf;\n"
        + "  padding: 20px 0 10px;\n"
        + "}\n"
        + "@media print {\n"
        + "  body { background: #fff; padding: 0; }\n"
        + "  .report-header { border-radius: 0; }\n"
        + "  @page { margin: 15mm; }\n"
        + "}\n";
}
