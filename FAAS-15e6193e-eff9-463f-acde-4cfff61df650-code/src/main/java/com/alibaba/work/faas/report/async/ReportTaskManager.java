package com.alibaba.work.faas.report.async;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.*;

/**
 * 报表异步任务管理器。
 *
 * <p>FaaS 连接器受限于「10s 调用超时，600s 执行上限」的约束。
 * 此管理器负责将 PDF 生成的耗时操作放到后台线程执行，从而让 FaaS 入口
 * 在 10s 内返回结果给宜搭。</p>
 *
 * <h3>流程</h3>
 * <ol>
 *   <li>FaasEntry.execute() 创建运营报告记录 → 返回 formInstId（<10s）</li>
 *   <li>ReportTaskManager 提交后台任务继续生成 PDF</li>
 *   <li>后台任务：查询数据 → 生成 HTML+PDF → 上传附件 → 更新状态（<600s）</li>
 * </ol>
 *
 * <h3>兜底策略</h3>
 * <ul>
 *   <li>任务超时（默认 30 分钟）自动标记为失败</li>
 *   <li>异常自动捕获并更新状态为失败原因</li>
 * </ul>
 *
 * @author Senior Developer
 * 创建于 2026/07/07
 */
public class ReportTaskManager {

    // ========================================
    //  单例
    // ========================================

    public static final ReportTaskManager INSTANCE = new ReportTaskManager();

    private final YidaFormUpdater formUpdater;

    private ReportTaskManager() {
        this.formUpdater = YidaFormUpdater.INSTANCE;
    }


    // ========================================
    //  线程池
    // ========================================

    /**
     * 后台 PDF 生成线程池。
     * <p>核心 2 线程，最大 4 线程，队列 10 个任务。
     * FaaS 场景下并发量通常很低（定时任务），此配置足够。</p>
     */
    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(
            2, 4, 60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(10),
            new ThreadFactory() {
                private int count = 0;
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "report-async-" + (++count));
                    t.setDaemon(true);
                    return t;
                }
            },
            new ThreadPoolExecutor.CallerRunsPolicy()
    );

    /**
     * 默认任务超时（30 分钟）。
     * FaaS 函数执行上限为 600s，此值作为内部兜底。
     */
    private static final long DEFAULT_TASK_TIMEOUT_MS = 30 * 60 * 1000L;


    // ========================================
    //  任务定义
    // ========================================

    /**
     * 后台报表生成任务。
     * 实现类负责实际的 PDF 生成和附件上传逻辑。
     */
    @FunctionalInterface
    public interface ReportTask {
        /**
         * 执行后台任务。
         *
         * @param formInstId 运营报告表单实例 ID
         * @param formUpdater 表单更新器（用于上传附件和更新状态）
         * @throws Exception 任意异常会被自动捕获并标记为失败
         */
        void run(String formInstId, YidaFormUpdater formUpdater) throws Exception;
    }


    // ========================================
    //  提交任务
    // ========================================

    /**
     * 提交后台报表生成任务。
     *
     * <p>FaaS 入口方法调用此方法后应立即返回，不影响 10s 超时限制。</p>
     *
     * @param formInstId 运营报告表单实例 ID
     * @param task       后台任务（PDF 生成 + 上传）
     */
    public void submit(String formInstId, ReportTask task) {
        Future<?> future = executor.submit(() -> {
            try {
                System.out.println("[ReportTaskManager] 后台任务开始: " + formInstId);
                task.run(formInstId, formUpdater);
                System.out.println("[ReportTaskManager] 后台任务完成: " + formInstId);
            } catch (Exception e) {
                System.err.println("[ReportTaskManager] 后台任务失败: " + formInstId
                        + " -> " + e.getMessage());
                e.printStackTrace(System.err);
                // 更新状态为失败
                try {
                    formUpdater.updateReportRecord(formInstId,
                            Collections.<Map<String, String>>emptyList(), null, false, e.getMessage());
                } catch (Exception updateErr) {
                    System.err.println("[ReportTaskManager] 更新失败状态也失败了: "
                            + updateErr.getMessage());
                }
            }
        });

        // 超时兜底：30 分钟后如果还没完成，标记为失败
        executor.submit(() -> {
            try {
                future.get(DEFAULT_TASK_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                System.err.println("[ReportTaskManager] 任务超时: " + formInstId
                        + "（超过 " + (DEFAULT_TASK_TIMEOUT_MS / 1000 / 60) + " 分钟）");
                future.cancel(true);
                try {
                    formUpdater.updateReportRecord(formInstId,
                            Collections.<Map<String, String>>emptyList(), null, false, "生成超时（超过30分钟）");
                } catch (Exception updateErr) {
                    System.err.println("[ReportTaskManager] 超时状态更新失败: "
                            + updateErr.getMessage());
                }
            } catch (Exception e) {
                // future.get 已包含执行异常，ReportTask 的 catch 已处理
            }
        });
    }
}
