package com.alibaba.work.faas.controller;

import com.alibaba.work.faas.report.ReportDateUtils;
import com.alibaba.work.faas.report.ReportService;
import com.alibaba.work.faas.report.async.YidaFormUpdater;
import com.alibaba.work.faas.report.model.ReportRequest;
import com.alibaba.work.faas.report.model.ReportResult;
import com.alibaba.work.faas.report.model.ReportType;
import com.alibaba.work.faas.report.model.TimeRange;
import com.alibaba.work.faas.service.OperationLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 手动生成报告 API。
 *
 * @author Senior Developer
 * 创建于 2026/07/08
 */
@RestController
@RequestMapping("/api/admin/reports")
public class ReportGenerateController {

    private static final Logger log = LoggerFactory.getLogger(ReportGenerateController.class);

    private final ReportService reportService;
    private final YidaFormUpdater formUpdater;
    private final OperationLogService operationLogService;

    /** 预定义任务：period → { timeRangeCode, label } */
    private static final Map<String, String[]> PERIOD_MAP = new LinkedHashMap<>();
    static {
        PERIOD_MAP.put("weekly",    new String[]{"lastWeek",    "周报"});
        PERIOD_MAP.put("monthly",   new String[]{"lastMonth",   "月报"});
        PERIOD_MAP.put("quarterly", new String[]{"lastQuarter", "季报"});
    }

    public ReportGenerateController(ReportService reportService,
                                     YidaFormUpdater formUpdater,
                                     OperationLogService operationLogService) {
        this.reportService = reportService;
        this.formUpdater = formUpdater;
        this.operationLogService = operationLogService;
    }

    /**
     * 手动生成报告。
     */
    @PostMapping("/generate")
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

            String platName = "平台运营报告-" + periodLabel + "-" + rangeLabel;
            String projName = "全项目汇总报告-" + rangeLabel;

            Map<String, Object> details = new LinkedHashMap<>();

            // 平台报告
            Map<String, Object> platResult = generateOne(ReportType.PLATFORM, tr, platName, "平台报告", dateDisplay, rangeLabel);
            details.put("platform", platResult);

            // 项目报告
            Map<String, Object> projResult = generateOne(ReportType.PROJECT, tr, projName, "项目报告", dateDisplay, rangeLabel);
            details.put("project", projResult);

            long totalCost = System.currentTimeMillis() - startAll;
            operationLogService.log("admin", "REPORT_MANUAL",
                    "手动生成-" + periodLabel + " 耗时" + totalCost + "ms", "SUCCESS", totalCost);

            result.put("success", true);
            result.put("period", period);
            result.put("label", periodLabel);
            result.put("details", details);
            result.put("cost", totalCost);
            log.info("✅ 手动生成 {} 完成，耗时 {}ms", periodLabel, totalCost);

        } catch (Exception e) {
            log.error("手动生成 {} 失败: {}", periodLabel, e.getMessage(), e);
            result.put("success", false);
            result.put("message", "生成失败: " + e.getMessage());
        }

        return result;
    }

    private Map<String, Object> generateOne(ReportType type, TimeRange tr,
                                              String reportName, String reportTypeName,
                                              String dateDisplay, String rangeLabel) {
        Map<String, Object> info = new LinkedHashMap<>();
        long stepStart = System.currentTimeMillis();

        try {
            ReportRequest request = new ReportRequest(type, Collections.singletonList(tr), null, null);
            List<ReportResult> results = reportService.generate(request);

            String formInstId = formUpdater.createReportRecord(reportName, reportTypeName, rangeLabel, dateDisplay);

            if (results.isEmpty()) {
                String remark = "该时间范围内无数据 | " + dateDisplay;
                formUpdater.updateReportRecord(formInstId, null, dateDisplay, true, remark);
                info.put("status", "no_data");
                info.put("message", "无数据");
                return info;
            }

            ReportResult rr = results.get(0);
            String obsName = reportTypeName.replace(" ", "") + "_" + tr.getCode();
            Map<String, String> obsInfo = formUpdater.uploadToObs(rr.getPdfBytes(), reportTypeName, obsName);
            formUpdater.updateReportRecord(formInstId, Collections.singletonList(obsInfo), dateDisplay, true, "已完成");

            info.put("status", "success");
            info.put("records", rr.getTotalRecords());
            info.put("pdfSize", rr.getPdfSize());
            info.put("obsUrl", obsInfo.get("previewUrl"));
            info.put("cost", System.currentTimeMillis() - stepStart);

        } catch (Exception e) {
            info.put("status", "error");
            info.put("message", e.getMessage());
            log.error("{} 生成失败: {}", reportName, e.getMessage());
        }

        return info;
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
