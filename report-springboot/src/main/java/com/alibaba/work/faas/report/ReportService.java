package com.alibaba.work.faas.report;

import com.alibaba.work.faas.report.async.YidaFormUpdater;
import com.alibaba.work.faas.report.model.ReportRequest;
import com.alibaba.work.faas.report.model.ReportResult;
import com.alibaba.work.faas.report.model.ReportType;
import com.alibaba.work.faas.report.model.TimeRange;
import com.alibaba.work.faas.report.strategy.PlatformReportStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 报表服务门面 —— 对外统一入口。
 *
 * <p>采用<strong>门面模式</strong>，内部委托给
 * {@link ReportStrategyFactory} + 各 {@link com.alibaba.work.faas.report.strategy.ReportStrategy} 实现。
 * 外部调用者无需关心策略选择逻辑。</p>
 *
 * @author Senior Developer
 * 创建于 2026/07/07
 */
@Service
public class ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportService.class);

    private final ReportStrategyFactory factory;
    private final PlatformReportStrategy platformStrategy;
    private final YidaFormUpdater formUpdater;

    public ReportService(ReportStrategyFactory factory,
                          PlatformReportStrategy platformStrategy,
                          YidaFormUpdater formUpdater) {
        this.factory = factory;
        this.platformStrategy = platformStrategy;
        this.formUpdater = formUpdater;
    }


    // ========================================
    //  新版入口：通过 ReportRequest 统一参数
    // ========================================

    /**
     * 根据请求参数生成报表。
     * <p>支持平台报告/项目报告，每个时间范围生成一份 PDF。</p>
     *
     * @param request 报表请求参数（含类型 + 时间范围）
     * @return 每个时间范围对应的报表结果列表
     * @throws Exception 查询或生成失败时抛出
     */
    public List<ReportResult> generate(ReportRequest request) throws Exception {
        return factory.execute(request);
    }


    // ========================================
    //  合并周期报告（周报/月报/季报）
    //  生成一份 PDF：平台报告（无页码）+ 全项目报告（有页码 1/N）
    // ========================================

    /**
     * 生成合并的周期报告（平台 + 全项目合并在一个 PDF 中）。
     *
     * @param tr            时间范围
     * @param periodLabel   周期标签（周报/月报/季报）
     * @param rangeLabel    范围标签（如 "6月第1周"）
     * @param dateDisplay   日期显示（如 "2026-06-01 ~ 2026-06-07"）
     * @return 合并后的 PDF 字节数组和 OBS 信息；如果总无数据则返回 null
     */
    public Map<String, Object> generateCombinedPeriodReport(TimeRange tr,
                                                              String periodLabel,
                                                              String rangeLabel,
                                                              String dateDisplay) throws Exception {
        String shortCode = UUID.randomUUID().toString().replace("-", "").substring(0, 4);
        String reportBaseName = "运营报告-" + periodLabel + "-" + rangeLabel
                + "-(" + dateDisplay + ")";

        log.info("▶ 生成合并报告: {}", reportBaseName);

        // 1. 生成平台报告数据
        ReportRequest platRequest = new ReportRequest(
                ReportType.PLATFORM, Collections.singletonList(tr), null, null);
        List<ReportResult> platResults = generate(platRequest);
        byte[] platformPdf = (platResults != null && !platResults.isEmpty())
                ? platResults.get(0).getPdfBytes() : null;

        // 2. 生成全项目报告数据
        ReportRequest projRequest = new ReportRequest(
                ReportType.PROJECT, Collections.singletonList(tr), null, null);
        List<ReportResult> projResults = generate(projRequest);
        byte[] projectPdf = (projResults != null && !projResults.isEmpty())
                ? projResults.get(0).getPdfBytes() : null;

        if (platformPdf == null && projectPdf == null) {
            log.warn("⚠ 平台报告和项目报告均无数据，跳过");
            return null;
        }

        // 3. 合并 PDF
        byte[] combinedPdf = PdfMerger.merge(
                platformPdf != null ? platformPdf : new byte[0],
                projectPdf != null ? projectPdf : new byte[0]);

        // 4. 创建宜搭记录
        String formInstId = formUpdater.createReportRecord(
                reportBaseName, periodLabel, rangeLabel, dateDisplay);

        if (formInstId == null || formInstId.isEmpty()) {
            log.error("创建宜搭记录失败，跳过 {}", reportBaseName);
            return null;
        }

        // 5. 上传到 OBS
        Map<String, String> obsInfo = formUpdater.uploadToObs(
                combinedPdf, periodLabel + "报告", reportBaseName);

        // 6. 更新宜搭记录
        formUpdater.updateReportRecord(formInstId,
                Collections.singletonList(obsInfo), dateDisplay, true, "已完成");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("reportName", reportBaseName);
        result.put("formInstId", formInstId);
        result.put("obsUrl", obsInfo.get("previewUrl"));
        result.put("pdfSize", combinedPdf.length);
        result.put("shortCode", shortCode);

        log.info("✅ 合并报告完成: {} ({} KB, formId={})",
                reportBaseName, combinedPdf.length / 1024, formInstId);

        return result;
    }


    // ========================================
    //  向后兼容方法（委托给 PlatformReportStrategy）
    // ========================================

    /**
     * 加载平台报表数据。
     *
     * @param range 时间范围：week / lastWeek / month / lastMonth
     * @return 报表数据
     * @throws Exception 查询失败时抛出
     * @deprecated 请使用 {@link #generate(ReportRequest)} 代替
     */
    @Deprecated
    public ReportData loadStats(String range) throws Exception {
        return platformStrategy.loadStats(range);
    }


    // ========================================
    //  便捷方法：一步生成
    // ========================================

    /**
     * 从 FaaS 入参 Map 直接生成报表。
     * <p>组合了 {@link ReportRequest#fromInput(Map)} 和 {@link #generate(ReportRequest)}。</p>
     *
     * @param input FaaS 的 input Map
     * @return 报表结果列表
     * @throws Exception 查询或生成失败时抛出
     */
    public List<ReportResult> generateFromInput(Map<String, Object> input) throws Exception {
        ReportRequest request = ReportRequest.fromInput(input);
        return generate(request);
    }

    /**
     * 快捷方法：生成平台报表。
     *
     * @param timeRanges 时间范围列表
     * @return 报表结果列表
     */
    public List<ReportResult> generatePlatformReport(TimeRange... timeRanges) throws Exception {
        ReportRequest request = new ReportRequest(
                ReportType.PLATFORM,
                Arrays.asList(timeRanges),
                null, null);
        return generate(request);
    }
}
