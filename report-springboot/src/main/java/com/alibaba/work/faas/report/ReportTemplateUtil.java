package com.alibaba.work.faas.report;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 报表 HTML/PDF 构建器共享工具 —— 转义、时间、页脚。
 *
 * <p>从 {@link ReportProjectPdfBuilder}、{@link ReportProjectHtmlBuilder}、
 * {@link ReportPdfBuilder}、{@link ReportHtmlBuilder} 中提取，
 * 消除四个文件中重复的 escHtml() / nowStr() / buildFooter() / SRC 常量。</p>
 *
 * @author Senior Developer
 * 创建于 2026/07/09
 */
public final class ReportTemplateUtil {

    private ReportTemplateUtil() {}

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * HTML 转义（防止 XSS 和格式破坏）。
     * 转义 & < > " ' ` 六个字符。
     */
    public static String escHtml(String s) {
        if (s == null || s.isEmpty()) return "";
        return s
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
            .replace("`", "&#x60;");
    }

    /** 当前时间字符串（yyyy-MM-dd HH:mm:ss） */
    public static String nowStr() {
        return LocalDateTime.now().format(FMT);
    }

    /** 通用页脚 */
    public static String buildFooter() {
        return "<div class=\"footer-note\">本报告由系统自动生成 | 报告生成时间：" + nowStr() + "</div>\n";
    }
}
