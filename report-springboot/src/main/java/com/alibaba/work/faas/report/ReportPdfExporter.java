package com.alibaba.work.faas.report;

import com.alibaba.work.faas.report.model.ProjectReportData;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * 报表 PDF 导出服务。
 *
 * @author Senior Developer
 * 创建于 2026/07/06
 */
@Service
public class ReportPdfExporter {

    private static final Logger log = LoggerFactory.getLogger(ReportPdfExporter.class);

    public ReportPdfExporter() {}

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
            log.info("[ReportPdfExporter] 使用内置中文字体: " + extracted);
            return fontPath;
        }

        // ---- 2. 尝试系统字体路径 ----
        for (String path : FONT_SEARCH_PATHS) {
            File f = new File(path);
            if (f.exists() && f.isFile()) {
                fontPath = f.getAbsolutePath();
                log.info("[ReportPdfExporter] 使用中文字体: " + fontPath);
                return fontPath;
            }
        }

        // ---- 3. 都找不到 ----
        log.warn("[ReportPdfExporter] 警告: 未找到中文字体，PDF 中文可能显示为方框");
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
            log.error("[ReportPdfExporter] 提取内置字体失败: " + e.getMessage());
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
    //  便捷方法（已移除：请通过 ReportService.generate() 调用）
    // ========================================
}
