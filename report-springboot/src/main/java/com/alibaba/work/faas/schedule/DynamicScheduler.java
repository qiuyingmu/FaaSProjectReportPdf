package com.alibaba.work.faas.schedule;

import com.alibaba.work.faas.report.ReportParallel;
import com.alibaba.work.faas.report.ReportDateUtils;
import com.alibaba.work.faas.report.ReportService;
import com.alibaba.work.faas.report.async.YidaFormUpdater;
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

    // ========================================
    //  失败重试机制
    // ========================================

    /** 最大重试次数 */
    private static final int MAX_RETRIES = 3;

    /** 重试退避延迟（毫秒）：5分钟、15分钟、30分钟 */
    private static final long[] RETRY_DELAYS_MS = {300_000L, 900_000L, 1_800_000L};

    /** 重试任务队列：type → 重试信息 */
    private final Map<String, RetryInfo> retryQueue = new ConcurrentHashMap<>();

    /** 重试信息 */
    private static class RetryInfo {
        final TimeRange timeRange;
        final String periodLabel;
        final String rangeLabel;
        final String dateDisplay;
        int attempt;    // 已尝试次数
        long nextRetry; // 下次重试时间戳

        RetryInfo(TimeRange timeRange, String periodLabel,
                  String rangeLabel, String dateDisplay) {
            this.timeRange = timeRange;
            this.periodLabel = periodLabel;
            this.rangeLabel = rangeLabel;
            this.dateDisplay = dateDisplay;
            this.attempt = 0;
            this.nextRetry = 0;
        }
    }

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
                            new org.springframework.scheduling.support.CronTrigger(
                                    task.getCron(),
                                    java.util.TimeZone.getTimeZone("Asia/Shanghai"));
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
     * 删除一个定时任务：停止运行中的 Future，并从内存配置中移除。
     */
    public synchronized void deleteTask(String type) {
        stopTask(type);
        taskConfigs.remove(type);
        log.info("🗑 任务已删除: {}", type);
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
    public synchronized List<ScheduleTask> getTasks() {
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
     * 生成合并的周期报告（委托给 ReportService），失败时自动重试。
     */
    private void generateCombinedReport(TimeRange tr, String periodLabel,
                                         String rangeLabel, String dateRangeDisplay) {
        try {
            Map<String, Object> result = reportService.generateCombinedPeriodReport(
                    tr, periodLabel, rangeLabel, dateRangeDisplay);
            if (result != null && Boolean.TRUE.equals(result.get("success"))) {
                log.info("✅ 合并报告完成: {}", result.get("reportName"));
            }
        } catch (Exception e) {
            log.error("{} 合并报告失败: {}", periodLabel, e.getMessage(), e);
            // 同步重试（立即再试一次，应对部分瞬时错误）
            try {
                Thread.sleep(2000);
                Map<String, Object> retry = reportService.generateCombinedPeriodReport(
                        tr, periodLabel, rangeLabel, dateRangeDisplay);
                if (retry != null && Boolean.TRUE.equals(retry.get("success"))) {
                    log.info("✅ {} 同步重试成功: {}", periodLabel, retry.get("reportName"));
                    return;
                }
            } catch (Exception e2) {
                log.warn("{} 同步重试也失败，安排异步重试", periodLabel);
            }
            // 异步重试（指数退避）
            scheduleRetry(tr, periodLabel, rangeLabel, dateRangeDisplay);
        }
    }

    /**
     * 安排异步重试（最多 MAX_RETRIES 次，5分钟→15分钟→30分钟）。
     */
    private void scheduleRetry(TimeRange tr, String periodLabel,
                                String rangeLabel, String dateDisplay) {
        String key = periodLabel;
        RetryInfo info = retryQueue.get(key);
        if (info == null) {
            info = new RetryInfo(tr, periodLabel, rangeLabel, dateDisplay);
            retryQueue.put(key, info);
        }

        if (info.attempt >= MAX_RETRIES) {
            log.error("❌ {} 已重试 {} 次仍未成功，放弃重试", periodLabel, MAX_RETRIES);
            retryQueue.remove(key);
            return;
        }

        // 在 lambda 中使用 final 副本
        final RetryInfo finalInfo = info;
        final String finalKey = key;
        final long delay = RETRY_DELAYS_MS[info.attempt];
        info.attempt++;
        info.nextRetry = System.currentTimeMillis() + delay;

        log.warn("⏰ {} 将于 {} 分钟后第 {} 次重试（共 {} 次）",
                periodLabel, delay / 60000, finalInfo.attempt, MAX_RETRIES);

        taskScheduler.schedule(() -> {
            try {
                log.info("⏰ {} 开始第 {} 次重试...", periodLabel, finalInfo.attempt);
                Map<String, Object> result = reportService.generateCombinedPeriodReport(
                        finalInfo.timeRange, finalInfo.periodLabel, finalInfo.rangeLabel, finalInfo.dateDisplay);
                if (result != null && Boolean.TRUE.equals(result.get("success"))) {
                    log.info("✅ {} 重试成功！", periodLabel);
                    retryQueue.remove(finalKey);
                    return;
                }
            } catch (Exception e) {
                log.warn("{} 第 {} 次重试失败: {}", periodLabel, finalInfo.attempt, e.getMessage());
            }
            // 继续下一次重试，用原始参数重新入队
            scheduleRetry(finalInfo.timeRange, finalInfo.periodLabel, finalInfo.rangeLabel, finalInfo.dateDisplay);
        }, new java.util.Date(System.currentTimeMillis() + delay));
    }

    /**
     * 应用关闭时停止所有任务。
     */
    @Override
    public void destroy() {
        for (String type : runningTasks.keySet()) {
            stopTask(type);
        }
        // 关闭 ReportParallel 的查询线程池
        ReportParallel.shutdown();
        log.info("所有定时任务已停止");
    }
}
