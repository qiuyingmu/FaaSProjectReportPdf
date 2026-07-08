package com.alibaba.work.faas.report.model;

/**
 * 报表生成结果模型。
 *
 * <p>封装一次报表生成的完整产出：原始数据 + HTML 内容 + PDF 字节数组。
 * 每个 {@link TimeRange} 对应一个 ReportResult。</p>
 *
 * @author Senior Developer
 * 创建于 2026/07/07
 */
public class ReportResult {

    /** 对应的时间范围 */
    private final TimeRange timeRange;

    /** HTML 文件内容（UTF-8 编码） */
    private final String htmlContent;

    /** PDF 文件字节数组 */
    private final byte[] pdfBytes;

    /** PDF 文件大小（字节） */
    private final long pdfSize;

    /** 总记录数（用于返回给 FaaS 调用方统计） */
    private final int totalRecords;

    /** 时间范围标签（如 "第27周（6月29日~7月5日）"） */
    private final String timeRangeLabel;

    public ReportResult(TimeRange timeRange, String htmlContent, byte[] pdfBytes,
                        int totalRecords, String timeRangeLabel) {
        this.timeRange = timeRange;
        this.htmlContent = htmlContent;
        this.pdfBytes = pdfBytes;
        this.pdfSize = pdfBytes != null ? pdfBytes.length : 0;
        this.totalRecords = totalRecords;
        this.timeRangeLabel = timeRangeLabel;
    }

    // ========================================
    //  Getters
    // ========================================

    public TimeRange getTimeRange() { return timeRange; }
    public String getHtmlContent() { return htmlContent; }
    public byte[] getPdfBytes() { return pdfBytes; }
    public long getPdfSize() { return pdfSize; }
    public int getTotalRecords() { return totalRecords; }
    public String getTimeRangeLabel() { return timeRangeLabel; }

    /** Base64 编码的 PDF 内容（便于 FaaS 以 JSON 返回） */
    public String getPdfBase64() {
        return java.util.Base64.getEncoder().encodeToString(pdfBytes);
    }

    @Override
    public String toString() {
        return "ReportResult{timeRange=" + timeRange.getLabel()
                + ", totalRecords=" + totalRecords
                + ", pdfSize=" + pdfSize + " bytes"
                + "}";
    }
}
