package com.alibaba.work.faas.report;

import com.alibaba.work.faas.report.model.ReportRequest;
import com.alibaba.work.faas.report.model.ReportResult;
import com.alibaba.work.faas.report.model.ReportType;
import com.alibaba.work.faas.report.strategy.PlatformReportStrategy;
import com.alibaba.work.faas.report.strategy.ProjectReportStrategy;
import com.alibaba.work.faas.report.strategy.ReportStrategy;

import java.util.List;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 报表策略工厂 —— 工厂模式 + 注册表模式。
 *
 * <p>根据 {@link ReportType} 返回对应的 {@link ReportStrategy} 实现。
 * 策略实例采用饿汉式单例，工厂内部使用注册表缓存。</p>
 *
 * <h3>扩展方式</h3>
 * <p>新增报表类型只需：</p>
 * <ol>
 *   <li>在 {@link ReportType} 中新增枚举值</li>
 *   <li>实现 {@link ReportStrategy} 接口</li>
 *   <li>在 {@link #initDefaults()} 中注册</li>
 * </ol>
 *
 * @author Senior Developer
 * 创建于 2026/07/07
 */
public class ReportStrategyFactory {

    // ========================================
    //  单例
    // ========================================

    public static final ReportStrategyFactory INSTANCE = new ReportStrategyFactory();

    private ReportStrategyFactory() {
        initDefaults();
    }

    // ========================================
    //  注册表
    // ========================================

    private final Map<ReportType, ReportStrategy> registry = new ConcurrentHashMap<>();

    /**
     * 注册默认策略实现。
     */
    private void initDefaults() {
        registry.put(ReportType.PLATFORM, PlatformReportStrategy.INSTANCE);
        registry.put(ReportType.PROJECT, ProjectReportStrategy.INSTANCE);
    }

    /**
     * 注册（或覆盖）某个报表类型的策略实现。
     * 可用于单元测试 mock 或动态切换实现。
     */
    public void register(ReportType type, ReportStrategy strategy) {
        if (type == null || strategy == null) {
            throw new IllegalArgumentException("type 和 strategy 不能为 null");
        }
        registry.put(type, strategy);
    }

    /**
     * 根据报表类型获取对应的策略实现。
     *
     * @param type 报表类型
     * @return 报表策略
     * @throws IllegalArgumentException 未注册该类型时抛出
     */
    public ReportStrategy getStrategy(ReportType type) {
        ReportStrategy strategy = registry.get(type);
        if (strategy == null) {
            throw new IllegalArgumentException("未注册的报表类型: " + type
                    + "，已注册: " + registry.keySet());
        }
        return strategy;
    }

    /**
     * 一站式方法：获取策略并执行。
     *
     * @param request 报表请求参数
     * @return 报表结果列表
     */
    public List<ReportResult> execute(ReportRequest request) throws Exception {
        ReportStrategy strategy = getStrategy(request.getType());
        return strategy.execute(request);
    }
}
