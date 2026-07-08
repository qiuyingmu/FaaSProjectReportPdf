package com.alibaba.work.faas.report.strategy;

import com.alibaba.work.faas.report.model.ReportRequest;
import com.alibaba.work.faas.report.model.ReportResult;

import java.util.List;

/**
 * 报表策略接口 —— 策略模式核心。
 *
 * <p>每种报表类型（平台报告/项目报告）实现此接口，
 * 通过 {@link com.alibaba.work.faas.report.ReportStrategyFactory} 获取对应策略实例。</p>
 *
 * <h3>职责</h3>
 * <ol>
 *   <li>根据请求参数查询数据</li>
 *   <li>生成 HTML 内容</li>
 *   <li>导出 PDF</li>
 *   <li>返回结果列表（每个时间范围一个结果）</li>
 * </ol>
 *
 * <h3>模板方法流程</h3>
 * <pre>
 * execute(request)
 *   ├── for each timeRange in request.getTimeRanges()
 *   │   ├── queryData(timeRange)          ← 子类实现
 *   │   ├── buildHtml(data)               ← 共享 builder
 *   │   └── exportPdf(html)               ← 共享 exporter
 *   └── return results
 * </pre>
 *
 * @author Senior Developer
 * 创建于 2026/07/07
 */
public interface ReportStrategy {

    /**
     * 执行报表生成。
     *
     * @param request 报表请求参数
     * @return 每个时间范围对应的报表结果列表
     * @throws Exception 查询或生成失败时抛出
     */
    List<ReportResult> execute(ReportRequest request) throws Exception;
}
