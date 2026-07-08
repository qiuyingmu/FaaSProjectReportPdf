package com.alibaba.work.faas;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import com.alibaba.work.faas.common.AbstractEntry;
import com.alibaba.work.faas.common.FaasInputs;
import com.alibaba.work.faas.report.ReportDateUtils;
import com.alibaba.work.faas.report.ReportService;
import com.alibaba.work.faas.report.async.ReportTaskManager;
import com.alibaba.work.faas.report.async.YidaFormUpdater;
import com.alibaba.work.faas.report.model.ReportRequest;
import com.alibaba.work.faas.report.model.ReportResult;
import com.alibaba.work.faas.report.model.ReportType;
import com.alibaba.work.faas.report.model.TimeRange;
import com.alibaba.work.faas.util.DingOpenApiUtil;
import com.alibaba.work.faas.util.YidaConnectorUtil;

/**
 * 宜搭 FaaS 连接器入口 —— 异步两阶段报表生成（OBS 存储版）。
 *
 * <h3>内存策略</h3>
 * <p>FaaS 容器内存有限，PDF/HTML 全程在内存中处理，不写磁盘。
 * 为避免同时持有多个 PDF 的大字节数组，后台线程按时间范围逐个生成、
 * 逐个上传到华为 OBS，每个 PDF 上传后引用即释放，交给 GC 回收。
 * 任何时候最多只有一个 PDF 字节数组在内存中。</p>
 *
 * <h3>流程</h3>
 * <p><b>阶段一（同步，<10s）：</b>解析参数 → 创建运营报告记录 → 提交后台任务 → 返回</p>
 * <p><b>阶段二（后台线程，<600s）：</b>逐个时间范围：查询数据 → 生成 PDF → 上传 OBS → 更新记录状态</p>
 *
 * <p>入参格式请参考 {@link ReportRequest}。</p>
 */
public class FaasEntry extends AbstractEntry {

    private final ReportService reportService = ReportService.INSTANCE;
    private final YidaFormUpdater formUpdater = YidaFormUpdater.INSTANCE;
    private final ReportTaskManager taskManager = ReportTaskManager.INSTANCE;

    @Override
    public JSONObject execute(FaasInputs faasInputs) {
        System.out.println("[FaasEntry] faasInputs: " + JSON.toJSONString(faasInputs));

        // ========================================
        //  必须保留：初始化宜搭工具类上下文
        // ========================================
        initYidaUtil(faasInputs);

        // ========================================
        //  提取业务入参
        // ========================================
        Map<String, Object> input = faasInputs.getInputs();
        System.out.println("[FaasEntry] 业务入参: " + JSON.toJSONString(input));

        JSONObject result = new JSONObject();

        try {
            // ---- 1. 解析参数 ----
            ReportRequest request = ReportRequest.fromInput(input);
            String timeRangesStr = input.containsKey("timeRanges")
                    ? (String) input.get("timeRanges") : "";

            // ---- 2. 报告名称 ----
            String reportTypeName = request.isPlatformReport() ? "平台报告" : "项目报告";

            // ---- 3. 创建运营报告记录（阶段一，<10s） ----
            String formInstId = formUpdater.createReportRecord(
                    reportTypeName, reportTypeName, timeRangesStr, timeRangesStr);
            System.out.println("[FaasEntry] 创建运营报告记录: " + formInstId);

            // ---- 4. 提交后台任务：逐个时间范围生成 PDF 并上传 OBS（阶段二，<600s） ----
            taskManager.submit(formInstId, (fid, updater) -> {
                // 收集所有 OBS 文件信息，最后一次性更新
                java.util.List<Map<String, String>> obsFiles = new java.util.ArrayList<>();
                // 计算日期范围字段值：yyyy-MM-dd ~ yyyy-MM-dd 格式
                java.util.List<String> dateLabels = new ArrayList<>();

                // 逐个时间范围处理，避免同时持有多个 PDF 的大字节数组
                for (TimeRange tr : request.getTimeRanges()) {
                    long stepStart = System.currentTimeMillis();

                    // 4a. 创建单时间范围的请求（避免一次生成全部）
                    ReportRequest singleReq = new ReportRequest(
                            request.getType(),
                            Arrays.asList(tr),
                            request.getProjectId(),
                            request.getProjectName());

                    // 4b. 生成 PDF（此过程中内存只有当前一个 PDF）
                    List<ReportResult> results = reportService.generate(singleReq);
                    if (results.isEmpty()) {
                        System.err.println("[FaasEntry] 跳过空结果: " + tr.getLabel());
                        continue;
                    }
                    ReportResult rr = results.get(0);

                    // 4c. 上传到华为 OBS（上传完成后释放 byte[] 引用）
                    String obsReportName = reportTypeName.replace(" ", "")
                            + "_" + rr.getTimeRange().getCode();
                    Map<String, String> obsInfo = updater.uploadToObs(
                            rr.getPdfBytes(), reportTypeName, obsReportName);
                    obsFiles.add(obsInfo);

                    // 4d. 计算日期范围：yyyy-MM-dd ~ yyyy-MM-dd
                    ReportDateUtils.DateRange dr = ReportDateUtils.getRange(tr.getCode());
                    if (dr != null) {
                        dateLabels.add(ReportDateUtils.fd(dr.start)
                                + " ~ " + ReportDateUtils.fd(dr.end));
                    }

                    long cost = System.currentTimeMillis() - stepStart;
                    System.out.println("[FaasEntry] 完成: " + obsReportName
                            + " (" + rr.getTotalRecords() + " 条, "
                            + rr.getPdfSize() / 1024 + " KB, 耗时 " + cost + "ms)");
                    System.out.println("[FaasEntry] OBS路径: " + obsInfo.get("objectName"));

                    // rr 和 pdfBytes 离开作用域 → GC 可回收
                }

                // 4e. 一次性更新：OBS 附件 + 日期范围 + 状态
                if (!obsFiles.isEmpty()) {
                    String computedDateRange = String.join(", ", dateLabels);
                    updater.updateReportRecord(fid, obsFiles, computedDateRange,
                            true, "已完成");
                }
            });

            // ---- 5. 立即返回（不阻塞） ----
            result.put("success", true);
            result.put("result", formInstId);
            result.put("count", request.getTimeRanges().size());
            result.put("message", "报告已提交生成，请稍后查看运营报告表单");
            result.put("error", "");

        } catch (Exception e) {
            System.err.println("[FaasEntry] 请求处理失败: " + e.getMessage());
            e.printStackTrace(System.err);
            result.put("success", false);
            result.put("result", null);
            result.put("error", e.getMessage());
        }

        System.out.println("[FaasEntry] 返回结果: " + result.toJSONString());
        return result;
    }


    /**
     * 将相关参数存到宜搭工具类里, 必须要调用此方法!!! 请不要删除!!!
     */
    private static final void initYidaUtil(FaasInputs faasInputs) {
        String accessToken = (String) faasInputs.getYidaContext().get("accessToken");
        DingOpenApiUtil.setAccessToken(accessToken);

        String consumeCode = (String) faasInputs.getYidaContext().get("consumeCode");
        YidaConnectorUtil.setConsumeCode(consumeCode);
    }
}
