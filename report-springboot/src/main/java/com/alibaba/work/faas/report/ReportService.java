package com.alibaba.work.faas.report;

import com.alibaba.work.faas.report.model.ReportRequest;
import com.alibaba.work.faas.report.model.ReportResult;
import com.alibaba.work.faas.report.model.ReportType;
import com.alibaba.work.faas.report.model.TimeRange;
import com.alibaba.work.faas.report.strategy.PlatformReportStrategy;
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

    private final ReportStrategyFactory factory;
    private final PlatformReportStrategy platformStrategy;

    public ReportService(ReportStrategyFactory factory,
                          PlatformReportStrategy platformStrategy) {
        this.factory = factory;
        this.platformStrategy = platformStrategy;
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
