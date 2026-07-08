package com.alibaba.work.faas;

import com.alibaba.fastjson.JSON;
import com.alibaba.work.faas.report.ReportProjectHtmlBuilder;
import com.alibaba.work.faas.report.ReportProjectPdfBuilder;
import com.alibaba.work.faas.report.ReportPdfExporter;
import com.alibaba.work.faas.report.model.ProjectReportData;
import com.alibaba.work.faas.report.model.ReportRequest;
import com.alibaba.work.faas.report.model.ReportResult;
import com.alibaba.work.faas.report.model.ReportType;
import com.alibaba.work.faas.report.model.TimeRange;
import com.alibaba.work.faas.report.strategy.ProjectReportStrategy;
import com.alibaba.work.faas.service.YidaApiManager;
import com.alibaba.work.faas.util.DingOpenApiUtil;
import com.alibaba.work.faas.util.YidaConnectorUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 本地项目报告生成测试 —— 仅生成 HTML 和 PDF，不操作宜搭表单，不上传 OBS。
 *
 * <p>运行方式（在项目根目录）：
 * <pre>
 * mvn compile exec:java -Dexec.mainClass="com.alibaba.work.faas.LocalProjectReportTest"
 * </pre>
 *
 * <p>输出到 {@code test-output/} 目录。</p>
 */
public class LocalProjectReportTest {

    public static void main(String[] args) throws Exception {
        System.out.println("================================================");
        System.out.println("  本地项目报告生成测试（仅 HTML + PDF）");
        System.out.println("================================================");

        // ========================================
        //  步骤 1：获取 accessToken
        // ========================================
        String accessToken;
        try {
            accessToken = YidaApiManager.INSTANCE.getAccessToken();
            System.out.println("[Test] ✅ 获取 accessToken 成功");
        } catch (Exception e) {
            System.err.println("[Test] ❌ 获取 accessToken 失败: " + e.getMessage());
            return;
        }

        // ========================================
        //  步骤 2：模拟 FaaS 运行时上下文
        // ========================================
        DingOpenApiUtil.setAccessToken(accessToken);
        YidaConnectorUtil.setConsumeCode("local-test-" + System.currentTimeMillis());

        // ========================================
        //  步骤 3：构造 ReportRequest
        // ========================================
        String timeRangeParam = args.length > 0 ? args[0] : "lastMonth";
        // 传入 "projectName=xxx" 可选指定项目，默认全项目汇总
        String projectName = null;
        for (String arg : args) {
            if (arg.startsWith("projectName=")) {
                projectName = arg.substring("projectName=".length());
            }
        }

        List<TimeRange> ranges = TimeRange.parseList(timeRangeParam);
        ReportRequest request = new ReportRequest(
                ReportType.PROJECT, ranges,
                null, projectName);

        System.out.println("[Test] 入参: " + request);

        // ========================================
        //  步骤 4：生成项目报告
        // ========================================
        long allStart = System.currentTimeMillis();
        List<ReportResult> results = ProjectReportStrategy.INSTANCE.execute(request);
        long allCost = System.currentTimeMillis() - allStart;
        System.out.println();
        System.out.println("[Test] ✅ 全部完成（" + allCost + "ms），共 " + results.size() + " 份报告");

        // ========================================
        //  步骤 5：保存到 test-output
        // ========================================
        File outputDir = new File("test-output");
        if (!outputDir.exists()) outputDir.mkdirs();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String timestamp = sdf.format(new Date());

        for (int i = 0; i < results.size(); i++) {
            ReportResult rr = results.get(i);
            String rangeLabel = rr.getTimeRange().getLabel();
            String prefix = "项目报告_" + rangeLabel + "_" + timestamp + (results.size() > 1 ? "_" + (i + 1) : "");

            // --- HTML ---
            File htmlFile = new File(outputDir, prefix + ".html");
            String htmlContent = rr.getHtmlContent();
            if (htmlContent != null) {
                try (FileOutputStream fos = new FileOutputStream(htmlFile)) {
                    fos.write(htmlContent.getBytes(StandardCharsets.UTF_8));
                }
                System.out.println("[Test] 📄 HTML: " + htmlFile.getAbsolutePath()
                        + " (" + htmlContent.length() + " chars)");
            }

            // --- PDF ---
            byte[] pdfBytes = rr.getPdfBytes();
            if (pdfBytes != null && pdfBytes.length > 0) {
                File pdfFile = new File(outputDir, prefix + ".pdf");
                try (FileOutputStream fos = new FileOutputStream(pdfFile)) {
                    fos.write(pdfBytes);
                }
                System.out.println("[Test] 📕 PDF:  " + pdfFile.getAbsolutePath()
                        + " (" + (pdfBytes.length / 1024) + " KB)");
            }

            System.out.println("[Test]   记录数: " + rr.getTotalRecords());
        }

        System.out.println();
        System.out.println("================================================");
        System.out.println("  测试完成！文件保存在 test-output/ 目录");
        System.out.println("================================================");
    }
}
