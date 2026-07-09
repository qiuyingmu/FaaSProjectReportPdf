package com.alibaba.work.faas.report;

import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * PDF 合并工具 —— 将两个 PDF 合并为一个（平台报告 + 项目报告）。
 *
 * <p>使用 PDFBox 的 {@link PDFMergerUtility} 实现，
 * 合并后的 PDF 保留各自原有的页眉页脚内容。</p>
 *
 * @author Senior Developer
 * 创建于 2026/07/09
 */
public final class PdfMerger {

    private static final Logger log = LoggerFactory.getLogger(PdfMerger.class);

    private PdfMerger() {}

    /**
     * 将两个 PDF 合并为一个（按顺序追加）。
     *
     * @param firstPdf  第一个 PDF 字节数组（平台报告，无页码）
     * @param secondPdf 第二个 PDF 字节数组（项目报告，有独立页码 1/N）
     * @return 合并后的 PDF 字节数组
     */
    public static byte[] merge(byte[] firstPdf, byte[] secondPdf) throws IOException {
        if (firstPdf == null || firstPdf.length == 0) {
            log.warn("[PdfMerger] 第一个 PDF 为空，直接返回第二个");
            return secondPdf;
        }
        if (secondPdf == null || secondPdf.length == 0) {
            log.warn("[PdfMerger] 第二个 PDF 为空，直接返回第一个");
            return firstPdf;
        }

        long start = System.currentTimeMillis();

        PDFMergerUtility merger = new PDFMergerUtility();
        ByteArrayOutputStream baos = new ByteArrayOutputStream(65536);
        merger.setDestinationStream(baos);

        try (PDDocument firstDoc = PDDocument.load(new ByteArrayInputStream(firstPdf));
             PDDocument secondDoc = PDDocument.load(new ByteArrayInputStream(secondPdf))) {

            merger.appendDocument(firstDoc, secondDoc);
            firstDoc.save(baos);

            int totalPages = firstDoc.getNumberOfPages();
            log.info("[PdfMerger] 合并完成，共 {} 页（平台 {} 页 + 项目 {} 页），耗时 {}ms",
                    totalPages,
                    totalPages - secondDoc.getNumberOfPages(),
                    secondDoc.getNumberOfPages(),
                    System.currentTimeMillis() - start);

            return baos.toByteArray();
        }
    }
}
