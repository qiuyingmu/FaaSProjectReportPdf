package com.alibaba.work.faas.controller;

import com.alibaba.work.faas.report.ReportDateUtils;
import com.alibaba.work.faas.report.ReportService;
import com.alibaba.work.faas.report.model.TimeRange;
import com.alibaba.work.faas.service.OperationLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 手动生成报告 API —— 生成合并的周期报告（平台 + 全项目）。
 *
 * @author Senior Developer
 * 创建于 2026/07/08
 */
@RestController
@RequestMapping("/api/admin/reports")
public class ReportGenerateController {

    private static final Logger log = LoggerFactory.getLogger(ReportGenerateController.class);

    private final ReportService reportService;
    private final OperationLogService operationLogService;

    /** 预定义任务：period → { timeRangeCode, label } */
    private static final Map<String, String[]> PERIOD_MAP = new LinkedHashMap<>();
    static {
        PERIOD_MAP.put("weekly",    new String[]{"lastWeek",    "周报"});
        PERIOD_MAP.put("monthly",   new String[]{"lastMonth",   "月报"});
        PERIOD_MAP.put("quarterly", new String[]{"lastQuarter", "季报"});
    }

    public ReportGenerateController(ReportService reportService,
                                     OperationLogService operationLogService) {
        this.reportService = reportService;
        this.operationLogService = operationLogService;
    }

    /**
     * 手动生成合并的周期报告（平台 + 全项目）。
     */
    @PostMapping(value = "/generate", consumes = "application/json")
    public Map<String, Object> generate(@RequestBody Map<String, String> body) {
        String period = body.get("period");
        Map<String, Object> result = new LinkedHashMap<>();

        if (period == null || !PERIOD_MAP.containsKey(period)) {
            result.put("success", false);
            result.put("message", "无效的周期参数: " + period + "，可选: weekly, monthly, quarterly");
            return result;
        }

        String[] config = PERIOD_MAP.get(period);
        String timeRangeCode = config[0];
        String periodLabel = config[1];

        long startAll = System.currentTimeMillis();

        try {
            TimeRange tr = TimeRange.parseList(timeRangeCode).get(0);
            String rangeLabel = ReportDateUtils.rangeLabel(timeRangeCode);
            ReportDateUtils.DateRange dr = ReportDateUtils.getRange(timeRangeCode);
            String dateDisplay = (dr != null)
                    ? ReportDateUtils.fd(dr.start) + " ~ " + ReportDateUtils.fd(dr.end)
                    : periodLabel;

            // 生成合并报告（平台 + 全项目）
            Map<String, Object> combined = reportService.generateCombinedPeriodReport(
                    tr, periodLabel, rangeLabel, dateDisplay);

            long totalCost = System.currentTimeMillis() - startAll;

            if (combined == null) {
                result.put("success", false);
                result.put("message", "平台报告和项目报告均无数据");
            } else {
                operationLogService.log("admin", "REPORT_MANUAL",
                        "手动生成-" + periodLabel + " 耗时" + totalCost + "ms", "SUCCESS", totalCost);

                result.put("success", true);
                result.put("period", period);
                result.put("label", periodLabel);
                result.put("reportName", combined.get("reportName"));
                result.put("obsUrl", combined.get("obsUrl"));
                result.put("pdfSize", combined.get("pdfSize"));
                result.put("cost", totalCost);
                log.info("✅ 手动生成 {} 完成，耗时 {}ms", periodLabel, totalCost);
            }

        } catch (Exception e) {
            log.error("手动生成 {} 失败: {}", periodLabel, e.getMessage(), e);
            result.put("success", false);
            result.put("message", "生成失败: " + e.getMessage());
        }

        return result;
    }

    /** 获取可用的时间范围和标签 */
    @GetMapping("/periods")
    public Map<String, Object> periods() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        List<Map<String, String>> list = new ArrayList<>();
        for (Map.Entry<String, String[]> e : PERIOD_MAP.entrySet()) {
            Map<String, String> item = new LinkedHashMap<>();
            item.put("period", e.getKey());
            item.put("label", e.getValue()[1]);
            item.put("timeRangeCode", e.getValue()[0]);
            list.add(item);
        }
        result.put("periods", list);
        return result;
    }
}
