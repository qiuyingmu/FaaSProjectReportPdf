package com.alibaba.work.faas.schedule;

import com.alibaba.work.faas.report.PdfMerger;
import com.alibaba.work.faas.report.ReportDateUtils;
import com.alibaba.work.faas.report.ReportService;
import com.alibaba.work.faas.report.async.YidaFormUpdater;
import com.alibaba.work.faas.report.model.ReportRequest;
import com.alibaba.work.faas.report.model.ReportResult;
import com.alibaba.work.faas.report.model.ReportType;
import com.alibaba.work.faas.report.model.TimeRange;
import com.alibaba.work.faas.service.OperationLogService;
import com.alibaba.work.faas.service.ScheduleTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * 动态定时调度器 —— 可运行时启/停/修改定时任务。
 *
 * <p>替代 {@code @Scheduled} 硬编码注解，支持 REST API 动态管理：
 * 查询任务列表、启停任务、修改 Cron 表达式。</p>
 *
 * @author Senior Developer
 * 创建于 2026/07/07
 */
@Component
public class DynamicScheduler implements DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(DynamicScheduler.class);

    private final ReportService reportService;
    private final YidaFormUpdater formUpdater;
    private final TaskScheduler taskScheduler;
    private final ScheduleTaskService scheduleTaskService;
    private final OperationLogService operationLogService;

    public DynamicScheduler(ReportService reportService,
                             YidaFormUpdater formUpdater,
                             TaskScheduler taskScheduler,
                             ScheduleTaskService scheduleTaskService,
                             OperationLogService operationLogService) {
        this.reportService = reportService;
        this.formUpdater = formUpdater;
        this.taskScheduler = taskScheduler;
        this.scheduleTaskService = scheduleTaskService;
        this.operationLogService = operationLogService;
    }

    /** 当前运行中的任务 */
    private final Map<String, ScheduledFuture<?>> runningTasks = new ConcurrentHashMap<>();

    /** 任务配置（内存中，重启后恢复默认） */
    private final Map<String, ScheduleTask> taskConfigs = new LinkedHashMap<>();

    @PostConstruct
    public void init() {
        reloadTasks();
    }

    /**
     * 从数据库重新加载任务配置。
     * 由 DataInitializer 在首次创建默认任务后调用，也支持运行时热重载。
     */
    public synchronized void reloadTasks() {
        // 先停止所有运行中的任务
        for (ScheduledFuture<?> future : runningTasks.values()) {
            if (future != null && !future.isCancelled()) {
                future.cancel(false);
            }
        }
        runningTasks.clear();
        taskConfigs.clear();

        List<ScheduleTask> stored = scheduleTaskService.findAll();
        if (!stored.isEmpty()) {
            for (ScheduleTask t : stored) {
                taskConfigs.put(t.getType(), t);
            }
            log.info("从数据库加载 {} 个定时任务", stored.size());
        } else {
            log.warn("数据库无定时任务配置，检查 DataInitializer 是否运行");
            return;
        }

        // 启动已启用的任务
        for (ScheduleTask task : taskConfigs.values()) {
            if (task.isEnabled()) {
                startTask(task);
            }
        }

        log.info("================================================");
        log.info("  动态调度器已启动，共 {} 个任务", taskConfigs.size());
        for (ScheduleTask t : taskConfigs.values()) {
            log.info("    {}: cron={} {}",
                    t.getDisplayName(), t.getCron(),
                    t.isEnabled() ? "🟢" : "🔴");
        }
        log.info("================================================");
    }

    // ========================================
    //  任务管理
    // ========================================

    /**
     * 添加一个已持久化的新任务到调度器。
     */
    public synchronized void addTask(ScheduleTask task) {
        taskConfigs.put(task.getType(), task);
        if (task.isEnabled()) {
            startTask(task);
        }
    }

    /**
     * 启动一个定时任务。
     */
    public synchronized void startTask(ScheduleTask task) {
        stopTask(task.getType()); // 先停止旧的

        Runnable runnable = createRunnable(task.getType());
        ScheduledFuture<?> future = taskScheduler.schedule(
                runnable,
                triggerContext -> {
                    org.springframework.scheduling.support.CronTrigger trigger =
                            new org.springframework.scheduling.support.CronTrigger(task.getCron());
                    return trigger.nextExecutionTime(triggerContext);
                });

        runningTasks.put(task.getType(), future);
        log.info("✅ 任务已启动: {} (cron: {})", task.getDisplayName(), task.getCron());
    }

    /**
     * 停止一个定时任务。
     */
    public synchronized void stopTask(String type) {
        ScheduledFuture<?> future = runningTasks.remove(type);
        if (future != null && !future.isCancelled()) {
            future.cancel(false);
            log.info("⏹ 任务已停止: {}", type);
        }
    }

    /**
     * 更新任务配置并重启。
     */
    public synchronized ScheduleTask updateTask(ScheduleTask task) {
        taskConfigs.put(task.getType(), task);
        if (task.isEnabled()) {
            startTask(task);
        } else {
            stopTask(task.getType());
        }
        // 持久化到数据库
        scheduleTaskService.save(task);
        return task;
    }

    /**
     * 获取所有任务配置（含运行状态）。
     */
    public List<ScheduleTask> getTasks() {
        List<ScheduleTask> result = new ArrayList<>();
        for (ScheduleTask t : taskConfigs.values()) {
            ScheduleTask copy = new ScheduleTask(
                    t.getType(), t.getCron(), t.getTimeRangeCode(), t.isEnabled(),
                    t.getDisplayName(), t.getDescription());
            copy.setEnabled(runningTasks.containsKey(t.getType()));
            result.add(copy);
        }
        return result;
    }


    // ========================================
    //  任务执行逻辑
    // ========================================

    private Runnable createRunnable(String type) {
        return () -> {
            ScheduleTask config = taskConfigs.get(type);
            if (config == null) {
                log.error("任务配置不存在: {}", type);
                return;
            }

            String timeRangeCode = config.getTimeRangeCode();
            if (timeRangeCode == null || timeRangeCode.isEmpty()) {
                log.error("任务 {} 未配置时间范围", type);
                return;
            }

            String periodLabel = config.getDisplayName() != null
                    ? config.getDisplayName() : type;

            long startAll = System.currentTimeMillis();
            log.info("===== 开始执行 {} =====", periodLabel);

            try {
                // 解析时间范围
                List<TimeRange> timeRanges = TimeRange.parseList(timeRangeCode);
                if (timeRanges.isEmpty()) {
                    log.error("时间范围解析失败: {}", timeRangeCode);
                    return;
                }
                TimeRange tr = timeRanges.get(0);

                String rangeLabel = ReportDateUtils.rangeLabel(timeRangeCode);
                ReportDateUtils.DateRange dr = ReportDateUtils.getRange(timeRangeCode);
                String dateRangeDisplay = (dr != null)
                        ? ReportDateUtils.fd(dr.start) + " ~ " + ReportDateUtils.fd(dr.end)
                        : periodLabel;

                // 生成合并的周期报告（平台 + 全项目）
                generateCombinedReport(tr, periodLabel, rangeLabel, dateRangeDisplay);

                long cost = System.currentTimeMillis() - startAll;
                log.info("===== {} 全部完成，总耗时 {}ms =====", periodLabel, cost);

            } catch (Exception e) {
                log.error("{} 执行失败: {}", periodLabel, e.getMessage(), e);
            }
        };
    }

    /**
     * 生成合并的周期报告：平台报告 + 全项目报告合并在一个 PDF 中。
     * <p>
     * 流程：
     * 1. 生成平台报告 PDF（无页码）
     * 2. 生成项目报告 PDF（有页码 1/N）
     * 3. 合并两个 PDF
     * 4. 上传到 OBS 并更新宜搭记录
     * </p>
     */
    private void generateCombinedReport(TimeRange tr, String periodLabel,
                                         String rangeLabel, String dateRangeDisplay) {
        long stepStart = System.currentTimeMillis();
        try {
            // ---- 构建报告名称 ----
            String shortCode = UUID.randomUUID().toString().replace("-", "").substring(0, 4);
            String reportBaseName = "运营报告-" + periodLabel + "-" + rangeLabel
                    + "-(" + dateRangeDisplay + ")";
            String reportFileName = reportBaseName + "-" + shortCode + ".pdf";

            log.info("▶ 生成合并报告: {}", reportFileName);

            // ---- 1. 生成平台报告数据 ----
            ReportRequest platRequest = new ReportRequest(
                    ReportType.PLATFORM, Collections.singletonList(tr), null, null);
            List<ReportResult> platResults = reportService.generate(platRequest);
            byte[] platformPdf = (platResults != null && !platResults.isEmpty())
                    ? platResults.get(0).getPdfBytes()
                    : null;

            // ---- 2. 生成全项目报告数据 ----
            ReportRequest projRequest = new ReportRequest(
                    ReportType.PROJECT, Collections.singletonList(tr), null, null);
            List<ReportResult> projResults = reportService.generate(projRequest);
            byte[] projectPdf = (projResults != null && !projResults.isEmpty())
                    ? projResults.get(0).getPdfBytes()
                    : null;

            if (platformPdf == null && projectPdf == null) {
                log.warn("⚠ 平台报告和项目报告均无数据，跳过");
                return;
            }

            // ---- 3. 合并 PDF（平台在前，项目在后） ----
            byte[] combinedPdf = PdfMerger.merge(
                    platformPdf != null ? platformPdf : new byte[0],
                    projectPdf != null ? projectPdf : new byte[0]);

            log.info("✅ 合并报告完成: {} ({} KB)",
                    reportFileName, combinedPdf.length / 1024);

            // ---- 4. 创建宜搭记录 ----
            String formInstId = formUpdater.createReportRecord(
                    reportBaseName, periodLabel, rangeLabel, dateRangeDisplay);

            if (formInstId == null || formInstId.isEmpty()) {
                log.error("创建宜搭记录失败，跳过 {}", reportFileName);
                return;
            }
            log.info("✅ 创建宜搭记录: {}", formInstId);

            // ---- 5. 上传合并 PDF 到 OBS ----
            Map<String, String> obsInfo = formUpdater.uploadToObs(
                    combinedPdf, periodLabel + "报告", reportBaseName);

            log.info("✅ OBS 上传成功: {}", obsInfo.get("objectName"));

            // ---- 6. 更新宜搭记录（添加附件 + 标记完成） ----
            formUpdater.updateReportRecord(formInstId,
                    Collections.singletonList(obsInfo),
                    dateRangeDisplay, true, "已完成");

            log.info("✅ 合并报告保存完成（{}ms）", reportFileName,
                    System.currentTimeMillis() - stepStart);

        } catch (Exception e) {
            log.error("{} 合并报告失败: {}", periodLabel, e.getMessage(), e);
        }
    }

    /**
     * 应用关闭时停止所有任务。
     */
    @Override
    public void destroy() {
        for (String type : runningTasks.keySet()) {
            stopTask(type);
        }
        log.info("所有定时任务已停止");
    }
}
