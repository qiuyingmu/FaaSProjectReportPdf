package com.alibaba.work.faas.report;

import com.alibaba.work.faas.report.model.ProjectReportData;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * 报表 PDF 导出服务。
 *
 * <p>使用 Open HTML to PDF 引擎（基于 Apache PDFBox）将 HTML 报表渲染为 PDF。</p>
 *
 * <h3>中文字体策略（按优先级）</h3>
 * <ol>
 *   <li><b>bundled/msyh.ttc</b> — 项目自带字体（classpath 或 fonts/ 目录），FaaS 友好</li>
 *   <li><b>系统字体</b> — Windows/Mac 本地开发环境</li>
 * </ol>
 * <p>找不到字体时不会报错，仅警告。PDF 中文部分将显示为方框。</p>
 *
 * @author Senior Developer
 * 创建于 2026/07/06
 */
public class ReportPdfExporter {

    // ========================================
    //  饿汉式单例
    // ========================================

    public static final ReportPdfExporter INSTANCE = new ReportPdfExporter();

    private ReportPdfExporter() {}

    /**
     * 中文字体搜索路径（按优先级排列）。
     * 项目内置字体通过 classpath 提取，无需在此列出路径。
     */
    private static final String[] FONT_SEARCH_PATHS = {
        "C:\\Windows\\Fonts\\msyh.ttc",       // Windows Microsoft YaHei
        "C:\\Windows\\Fonts\\simhei.ttf",     // Windows 黑体
        "C:\\Windows\\Fonts\\simsun.ttc",     // Windows 宋体
    };

    /** 字体资源名称（classpath 路径） */
    private static final String FONT_CLASSPATH = "/fonts/msyh.ttc";

    /** 找到的中文字体路径（懒加载，首次调用时检测） */
    private volatile String fontPath;

    /** 提取到临时文件的内置字体路径（用完需清理） */
    private volatile String extractedFontPath;


    // ========================================
    //  字体检测
    // ========================================

    /**
     * 自动检测可用的中文字体。
     *
     * <p>检测顺序：</p>
     * <ol>
     *   <li>classpath 提取的内置字体（fonts/msyh.ttc）</li>
     *   <li>系统字体（Windows 路径等）</li>
     * </ol>
     */
    private String detectFont() {
        if (fontPath != null) return fontPath;

        // ---- 1. 尝试从 classpath 提取内置字体 ----
        // 适用于 FaaS 环境：字体打包在 JAR 中，运行时解到临时目录
        String extracted = extractBundledFont();
        if (extracted != null) {
            fontPath = extracted;
            System.out.println("[ReportPdfExporter] 使用内置中文字体: " + extracted);
            return fontPath;
        }

        // ---- 2. 尝试系统字体路径 ----
        for (String path : FONT_SEARCH_PATHS) {
            File f = new File(path);
            if (f.exists() && f.isFile()) {
                fontPath = f.getAbsolutePath();
                System.out.println("[ReportPdfExporter] 使用中文字体: " + fontPath);
                return fontPath;
            }
        }

        // ---- 3. 都找不到 ----
        System.out.println("[ReportPdfExporter] 警告: 未找到中文字体，PDF 中文可能显示为方框");
        return null;
    }

    /**
     * 从 classpath 提取内置字体到临时文件。
     * 适用于 FaaS 环境（类路径在 JAR 内，需要解到文件系统）。
     */
    private String extractBundledFont() {
        if (extractedFontPath != null) return extractedFontPath;

        try (InputStream is = getClass().getResourceAsStream(FONT_CLASSPATH)) {
            if (is == null) return null;

            File tempFile = File.createTempFile("yida-font-", ".ttc");
            tempFile.deleteOnExit();
            Files.copy(is, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            extractedFontPath = tempFile.getAbsolutePath();
            return extractedFontPath;
        } catch (Exception e) {
            System.out.println("[ReportPdfExporter] 提取内置字体失败: " + e.getMessage());
            return null;
        }
    }


    // ========================================
    //  核心方法
    // ========================================

    /**
     * 将平台报表数据导出为 PDF 字节数组。
     */
    public byte[] exportPdf(ReportData data) throws Exception {
        String xhtml = ReportPdfBuilder.INSTANCE.build(data);
        return exportPdfFromHtml(xhtml);
    }

    /**
     * 将项目报表数据导出为 PDF 字节数组。
     */
    public byte[] exportPdf(ProjectReportData data) throws Exception {
        String xhtml = ReportProjectPdfBuilder.INSTANCE.build(data);
        return exportPdfFromHtml(xhtml);
    }

    /**
     * 将任意 XHTML 字符串渲染为 PDF 字节数组。
     */
    public byte[] exportPdfFromHtml(String xhtml) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(65536);
        renderToStream(xhtml, baos);
        return baos.toByteArray();
    }

    /**
     * 将 XHTML 字符串渲染为 PDF 并写入输出流。
     */
    public void renderToStream(String xhtml, OutputStream out) throws Exception {
        PdfRendererBuilder builder = new PdfRendererBuilder();
        builder.useFastMode();

        // 嵌入中文字体
        String font = detectFont();
        if (font != null) {
            builder.useFont(new File(font), "Microsoft YaHei");
        }

        builder.withHtmlContent(xhtml, null);
        builder.toStream(out);
        builder.run();
    }


    // ========================================
    //  便捷方法
    // ========================================

    /**
     * 便捷方法：直接通过时间范围加载报表并导出 PDF。
     */
    public byte[] loadAndExport(String range) throws Exception {
        ReportData data = ReportService.INSTANCE.loadStats(range);
        return exportPdf(data);
    }
}
