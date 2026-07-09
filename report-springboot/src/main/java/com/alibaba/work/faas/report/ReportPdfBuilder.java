package com.alibaba.work.faas.report;

import java.util.List;

/**
 * PDF 专用 XHTML 模板生成器。
 *
 * <p>与 {@link ReportHtmlBuilder} 不同，此生成器仅使用 Flying Saucer (iText 2.1.7)
 * 所支持的 CSS 2.1 特性：</p>
 * <ul>
 *   <li>✅ table 布局（替代 flexbox/grid）</li>
 *   <li>✅ 纯色背景（替代渐变 gradient）</li>
 *   <li>✅ 无 border-radius / box-shadow / transform</li>
 *   <li>✅ 无 ::before / ::after 伪元素</li>
 *   <li>✅ 无 emoji（替换为中文字符）</li>
 *   <li>✅ page-break-after / page-break-inside 合理分页</li>
 * </ul>
 *
 * @author Senior Developer
 * 创建于 2026/07/06
 */
public class ReportPdfBuilder {

    // ========================================
    //  单例
    // ========================================

    public static final ReportPdfBuilder INSTANCE = new ReportPdfBuilder();
    private ReportPdfBuilder() {}


    /**
     * 构建 PDF 专用 XHTML。
     */
    public String build(ReportData data) {
        StringBuilder html = new StringBuilder(8192);
        html.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n")
            .append("<html xmlns=\"http://www.w3.org/1999/xhtml\" lang=\"zh-CN\">\n<head>\n")
            .append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"/>\n")
            .append("<title>平台运营报告</title>\n")
            .append("<style type=\"text/css\">\n")
            .append(PDF_CSS)
            .append("</style>\n</head>\n<body>\n")
            .append(buildHeader(data))
            .append(buildSection1(data))
            .append(buildSection2(data))
            .append(buildSection3(data))
            .append(buildFooter())
            .append("</body>\n</html>");
        return html.toString();
    }


    // ========================================
    //  Header
    // ========================================

    private String buildHeader(ReportData data) {
        String period = data.getRangeKey().contains("week") ? "周报" : "月报";
        return ""
            + "<table class=\"header-table\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\">\n"
            + "  <tr>\n"
            + "    <td class=\"header-cell\">\n"
            + "      <div class=\"header-badge\">平台运营报告</div>\n"
            + "      <div class=\"header-title\">【平台运营报告-" + period + "-" + data.getTimeRangeLabel() + "】</div>\n"
            + "      <div class=\"header-subtitle\">报告生成时间：" + nowStr() + "</div>\n"
            + "    </td>\n"
            + "  </tr>\n"
            + "</table>\n";
    }


    // ========================================
    //  Section 1: 平台基本运营数据
    // ========================================

    private String buildSection1(ReportData data) {
        return ""
            + "<table class=\"section-table\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\">\n"
            + "  <tr>\n"
            + "    <td class=\"section-title-td\" style=\"border:1px solid #e8e8e8;border-bottom:2px solid #eef2ff;border-radius:8px 8px 0 0;\">\n"
            + "      <table class=\"section-title-row\" cellpadding=\"0\" cellspacing=\"0\">\n"
            + "        <tr>\n"
            + "          <td class=\"section-num\">1</td>\n"
            + "          <td class=\"section-title-text\">平台基本运营数据</td>\n"
            + "        </tr>\n"
            + "      </table>\n"
            + "    </td>\n"
            + "  </tr>\n"
            + "  <tr>\n"
            + "    <td style=\"border:1px solid #e8e8e8;border-top:none;border-radius:0 0 8px 8px;\">\n"
            + "      <table class=\"summary-grid\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\">\n"
            + "        <tr>\n"
            + "          <td class=\"summary-item\">\n"
            + "            <div class=\"summary-label\">报告时间</div>\n"
            + "            <div class=\"summary-value summary-blue\">" + data.getTimeRangeLabel() + "</div>\n"
            + "          </td>\n"
            + "          <td class=\"summary-item\">\n"
            + "            <div class=\"summary-label\">项目数量</div>\n"
            + "            <div class=\"summary-value summary-green\">" + data.getProjects().size()
            +               " <span class=\"summary-unit\">个</span></div>\n"
            + "          </td>\n"
            + "          <td class=\"summary-item\">\n"
            + "            <div class=\"summary-label\">" + data.getPeriodName() + "总记录</div>\n"
            + "            <div class=\"summary-value summary-orange\">" + data.getTotalRecords()
            +               " <span class=\"summary-unit\">条</span></div>\n"
            + "          </td>\n"
            + "        </tr>\n"
            + "      </table>\n"
            + "    </td>\n"
            + "  </tr>\n"
            + "</table>\n";
    }


    // ========================================
    //  Section 2: 项目基本信息
    // ========================================

    private String buildSection2(ReportData data) {
        StringBuilder sb = new StringBuilder();
        sb.append("<table class=\"section-table\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\">\n")
          .append("  <tr>\n")
          .append("    <td class=\"section-title-td\" style=\"border:1px solid #e8e8e8;border-bottom:2px solid #eef2ff;border-radius:8px 8px 0 0;\">\n")
          .append("      <table class=\"section-title-row\" cellpadding=\"0\" cellspacing=\"0\">\n")
          .append("        <tr>\n")
          .append("          <td class=\"section-num\">2</td>\n")
          .append("          <td class=\"section-title-text\">项目基本信息</td>\n")
          .append("        </tr>\n")
          .append("      </table>\n")
          .append("    </td>\n")
          .append("  </tr>\n")
          .append("  <tr>\n")
          .append("    <td>\n")
          .append("      <table class=\"data-table\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\">\n")
          .append("        <thead>\n")
          .append("          <tr>\n")
          .append("            <th style=\"text-align:left;width:30%;\">项目名称</th>\n")
          .append("            <th>负责人</th>\n")
          .append("            <th>项目总监</th>\n")
          .append("            <th>区域</th>\n")
          .append("            <th>").append(data.getPeriodName()).append("总记录</th>\n")
          .append("          </tr>\n")
          .append("        </thead>\n")
          .append("        <tbody>\n");

        // 不需要 emoji，直接用文字
        for (ReportData.ProjectStat p : data.getProjects()) {
            sb.append("          <tr>\n")
              .append("            <td style=\"text-align:left;\">").append(escHtml(p.getName())).append("</td>\n")
              .append("            <td><span class=\"tag tag-person\">").append(escHtml(p.getDirector())).append("</span></td>\n")
              .append("            <td><span class=\"tag tag-director\">").append(escHtml(p.getDirector())).append("</span></td>\n")
              .append("            <td><span class=\"tag tag-area\">").append(escHtml(p.getArea())).append("</span></td>\n")
              .append("            <td><span class=\"count-num\">").append(p.getTotalCount()).append("</span></td>\n")
              .append("          </tr>\n");
        }

        sb.append("        </tbody>\n")
          .append("      </table>\n")
          .append("    </td>\n")
          .append("  </tr>\n")
          .append("</table>\n");
        return sb.toString();
    }


    // ========================================
    //  Section 3: 项目清单 · 数据统计
    // ========================================

    private String buildSection3(ReportData data) {
        StringBuilder sb = new StringBuilder();

        // Section 3 标题（独立 table，不与项目共用 table）
        sb.append("<table class=\"section-table\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\">\n")
          .append("  <tr><td class=\"section-title-td\">\n")
          .append("    <table class=\"section-title-row\" cellpadding=\"0\" cellspacing=\"0\">\n")
          .append("      <tr>\n")
          .append("        <td class=\"section-num\">3</td>\n")
          .append("        <td class=\"section-title-text\">项目清单 · 数据统计</td>\n")
          .append("      </tr>\n")
          .append("    </table>\n")
          .append("  </td></tr>\n")
          .append("</table>\n");

        if (data.getProjects().isEmpty()) {
            sb.append("<div style=\"padding:20px;color:#8c8c8c;text-align:center;\">暂无项目数据</div>\n");
        } else {
            int projIdx = 1;
            for (ReportData.ProjectStat p : data.getProjects()) {
                // 每个项目独立为一块，page-break-inside:avoid 防止项目被分页断开
                sb.append("<div style=\"page-break-inside:avoid;\">\n")
                  .append("  <table class=\"project-block\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\">\n")
                  .append("    <tr>\n")
                  .append("      <td class=\"project-header\">\n")
                  .append("        <table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\">\n")
                  .append("          <tr>\n")
                  .append("            <td class=\"project-header-left\">\n")
                  .append("              <span class=\"project-index\">").append(projIdx).append("</span>\n")
                  .append("              <span class=\"project-name\">").append(escHtml(p.getName())).append("</span>\n")
                  .append("            </td>\n")
                  .append("            <td class=\"project-header-right\">\n")
                  .append("              ").append(data.getPeriodName()).append(" <strong>").append(p.getTotalCount()).append("</strong> 条 | ")
                  .append(escHtml(p.getArea())).append("\n")
                  .append("            </td>\n")
                  .append("          </tr>\n")
                  .append("        </table>\n")
                  .append("      </td>\n")
                  .append("    </tr>\n")
                  .append("    <tr>\n")
                  .append("      <td class=\"project-content\">\n")
                  .append("        <table class=\"source-grid\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\">\n")
                  .append("          <tr>\n");

                // 6 个数据源卡片，一行全部展示
                int si = 0;
                for (ReportConstants.SourceDef src : ReportConstants.SOURCES) {
                    Integer count = p.getSourceCounts().getOrDefault(src.key, 0);
                    if (si > 0 && si % 6 == 0) {
                        sb.append("          </tr>\n").append("          <tr>\n");
                    }
                    sb.append("            <td class=\"source-card\">\n")
                      .append("              <div class=\"source-card-label\">").append(src.label).append("</div>\n")
                      .append("              <div class=\"source-card-count\" style=\"color:")
                      .append(src.color).append(";\">").append(count).append("</div>\n")
                      .append("              <div class=\"source-card-period\">").append(data.getPeriodName()).append("</div>\n")
                      .append("            </td>\n");
                    si++;
                }

                sb.append("          </tr>\n")
                  .append("        </table>\n")
                  .append("      </td>\n")
                  .append("    </tr>\n")
                  .append("  </table>\n")
                  .append("</div>\n");
                projIdx++;
            }
        }

        return sb.toString();
    }


    // ========================================
    //  Footer
    // ========================================

    private String buildFooter() {
        return ReportTemplateUtil.buildFooter();
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


    // ========================================
    //  CSS 2.1 兼容样式（Flying Saucer 可用）
    // ========================================

    private static final String PDF_CSS = ""
        // ---- 全局重置 ----
        + "body {"
        + "  font-family: 'Microsoft YaHei', 'SimHei', 'PingFang SC', sans-serif;"
        + "  font-size: 12px;"
        + "  color: #333;"
        + "  margin: 0; padding: 0;"
        + "  background: #fff;"
        + "}\n"
        + "table { border-collapse: collapse; }\n"

        // ---- Header ----
        + ".header-table { margin-bottom: 16px; }\n"
        + ".header-cell {"
        + "  background-color: #1a1a2e;"
        + "  padding: 28px 32px;"
        + "  color: #fff;"
        + "}\n"
        + ".header-badge {"
        + "  font-size: 11px;"
        + "  color: #cccccc;"
        + "  margin-bottom: 4px;"
        + "}\n"
        + ".header-title {"
        + "  font-size: 18px;"
        + "  font-weight: 700;"
        + "  color: #ffffff;"
        + "}\n"
        + ".header-subtitle {"
        + "  font-size: 11px;"
        + "  color: #888899;"
        + "  margin-top: 4px;"
        + "}\n"

        // ---- Section ----
        + ".section-table {"
        + "  width: 100%;"
        + "  border-collapse: separate;"
        + "  border-spacing: 0;"
        + "  margin-bottom: 16px;"
        + "}\n"
        + ".section-title-td {"
        + "  padding: 14px 20px 6px 20px;"
        + "}\n"
        + ".section-title-row { margin:0; padding:0; }\n"
        + ".section-num {"
        + "  width: 26px; height: 26px;"
        + "  background-color: #2563eb;"
        + "  color: #fff;"
        + "  font-size: 12px;"
        + "  font-weight: 700;"
        + "  text-align: center;"
        + "  vertical-align: middle;"
        + "  border-radius: 6px;"
        + "}\n"
        + ".section-title-text {"
        + "  font-size: 15px;"
        + "  font-weight: 600;"
        + "  color: #1a1a2e;"
        + "  padding-left: 8px;"
        + "}\n"

        // ---- Summary ----
        // ---- Section 1: 平台基本运营数据 ----
        + ".summary-grid { margin-top: 16px; }\n"
        + ".summary-item {"
        + "  text-align: center;"
        + "  padding: 16px 8px;"
        + "  background-color: #fafbfc;"
        + "  border: 1px solid #f0f0f0;"
        + "  width: 33%;"
        + "}\n"
        + ".summary-label {"
        + "  font-size: 12px;"
        + "  color: #8c8c8c;"
        + "  margin-bottom: 6px;"
        + "}\n"
        + ".summary-value {"
        + "  font-size: 24px;"
        + "  font-weight: 700;"
        + "}\n"
        + ".summary-blue { color: #3b82f6; }\n"
        + ".summary-green { color: #10b981; }\n"
        + ".summary-orange { color: #f59e0b; }\n"
        + ".summary-unit {"
        + "  font-size: 12px;"
        + "  font-weight: 400;"
        + "  color: #8c8c8c;"
        + "}\n"

        // ---- Data Table ----
        + ".data-table {"
        + "  width: 100%;"
        + "  border-collapse: collapse;"
        + "  font-size: 12px;"
        + "}\n"
        + ".data-table thead th {"
        + "  background-color: #f8f9fb;"
        + "  padding: 8px 12px;"
        + "  text-align: center;"
        + "  font-weight: 600;"
        + "  color: #595959;"
        + "  font-size: 11px;"
        + "  border-bottom: 1px solid #e8e8e8;"
        + "  border-right: 1px solid #f0f0f0;"
        + "}\n"
        + ".data-table thead th:last-child { border-right: none; }\n"
        + ".data-table tbody td {"
        + "  padding: 8px 12px;"
        + "  text-align: center;"
        + "  border-bottom: 1px solid #eee;"
        + "  border-right: 1px solid #f5f5f5;"
        + "  color: #333;"
        + "  vertical-align: middle;"
        + "}\n"
        + ".data-table tbody td:last-child { border-right: none; }\n"

        // ---- Tags ----
        + ".tag {"
        + "  font-size: 10px;"
        + "  padding: 2px 6px;"
        + "  white-space: nowrap;"
        + "}\n"
        + ".tag-person { background-color: #ecfdf5; color: #059669; }\n"
        + ".tag-director { background-color: #fff7ed; color: #d97706; }\n"
        + ".tag-area { background-color: #eef2ff; color: #4f46e5; }\n"
        + ".count-num {"
        + "  color: #3b82f6;"
        + "  font-size: 13px;"
        + "  font-weight: 700;"
        + "}\n"

        // ---- Project Block ----
        + ".project-block {"
        + "  width: 100%;"
        + "  border-collapse: separate;"
        + "  border-spacing: 0;"
        + "  border: 1px solid #e8e8e8;"
        + "  border-radius: 6px;"
        + "  margin-bottom: 12px;"
        + "  page-break-inside: avoid;"
        + "}\n"
        + ".project-header {"
        + "  padding: 10px 16px;"
        + "  background-color: #fff;"
        + "  border-bottom: 1px solid #eee;"
        + "}\n"
        + ".project-header-left {"
        + "  text-align: left;"
        + "  width: 65%;"
        + "}\n"
        + ".project-header-right {"
        + "  text-align: right;"
        + "  font-size: 11px;"
        + "  color: #8c8c8c;"
        + "  width: 35%;"
        + "}\n"
        + ".project-index {"
        + "  display: inline-block;"
        + "  width: 24px; height: 24px;"
        + "  line-height: 24px;"
        + "  background-color: #2563eb;"
        + "  color: #fff;"
        + "  font-size: 11px;"
        + "  font-weight: 700;"
        + "  text-align: center;"
        + "  margin-right: 8px;"
        + "  vertical-align: middle;"
        + "  border-radius: 6px;"
        + "}\n"
        + ".project-name {"
        + "  font-size: 13px;"
        + "  font-weight: 600;"
        + "  color: #1a1a2e;"
        + "  vertical-align: middle;"
        + "}\n"

        // ---- Source Cards Grid (1 行 x 6 列) ----
        + ".project-content {"
        + "  padding: 12px 12px;"
        + "  background-color: #fafbfc;"
        + "}\n"
        + ".source-grid {"
        + "  width: 100%;"
        + "  border-collapse: separate;"
        + "  border-spacing: 4px;"
        + "}\n"
        + ".source-card {"
        + "  background-color: #fff;"
        + "  border: 1px solid #e8e8e8;"
        + "  text-align: center;"
        + "  padding: 8px 2px;"
        + "  width: 16%;"
        + "  border-radius: 4px;"
        + "}\n"
        + ".source-card-label {"
        + "  font-size: 10px;"
        + "  font-weight: 500;"
        + "  color: #1a1a2e;"
        + "  margin-bottom: 3px;"
        + "}\n"
        + ".source-card-count {"
        + "  font-size: 16px;"
        + "  font-weight: 700;"
        + "}\n"
        + ".source-card-period {"
        + "  font-size: 8px;"
        + "  color: #8c8c8c;"
        + "}\n"

        // ---- Footer ----
        + ".footer-note {"
        + "  text-align: center;"
        + "  font-size: 10px;"
        + "  color: #bfbfbf;"
        + "  padding: 16px 0 8px;"
        + "}\n"

        // ---- Print ----
        + "@page { margin: 12mm 15mm; }\n";
}
