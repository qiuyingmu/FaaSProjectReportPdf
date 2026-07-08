package com.alibaba.work.faas.report;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;

/**
 * 并行查询工具 —— 将多个独立的数据源查询任务并行执行。
 *
 * <p>6 个数据源的 API 调用互不依赖，串行执行浪费 FaaS 容器的等待时间。
 * 此工具将查询任务提交到共享线程池，并行执行后收集结果。</p>
 *
 * <p>线程池使用守护线程，不会阻止 JVM 退出。</p>
 *
 * @author Senior Developer
 * 创建于 2026/07/07
 */
public final class ReportParallel {

    private ReportParallel() {}

    /** 共享线程池：核心 2 线程，最大 6（刚好 6 个数据源并行），队列 10 */
    private static final ExecutorService EXECUTOR = new ThreadPoolExecutor(
            2, 6, 60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(10),
            r -> {
                Thread t = new Thread(r, "report-query-" + System.nanoTime());
                t.setDaemon(true);
                return t;
            });

    private static final Logger log = LoggerFactory.getLogger(ReportParallel.class);

    /**
     * 并行执行多个独立任务，收集结果到有序 Map。
     *
     * <p>所有任务同时提交，等待全部完成后返回。
     * 某个任务失败不会影响其他任务，但异常会被记录并重抛。</p>
     *
     * @param items   输入列表（如 6 个数据源定义）
     * @param mapper  将输入映射为输出的函数（如查询方法）
     * @param <T>     输入类型（如 SourceDef）
     * @param <R>     输出类型（如 Map&lt;String, Integer&gt;）
     * @return 保持输入顺序的 LinkedHashMap（inputKey → result）
     * @throws Exception 任一任务失败时抛出
     */
    public static <T, R> Map<T, R> parallelMap(
            List<T> items,
            Function<T, R> mapper,
            String taskName) throws Exception {

        if (items == null || items.isEmpty()) return Collections.emptyMap();

        long start = System.currentTimeMillis();
        int total = items.size();

        // 提交所有任务
        List<CompletableFuture<Map.Entry<T, R>>> futures = new ArrayList<>(total);
        for (T item : items) {
            futures.add(CompletableFuture.supplyAsync(() -> {
                R result = mapper.apply(item);
                return new AbstractMap.SimpleEntry<>(item, result);
            }, EXECUTOR));
        }

        // 等待全部完成
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        } catch (CompletionException e) {
            throw new Exception("并行查询失败 [" + taskName + "]: " + e.getCause().getMessage(), e.getCause());
        }

        // 收集结果（保持输入顺序）
        Map<T, R> result = new LinkedHashMap<>(total);
        for (CompletableFuture<Map.Entry<T, R>> future : futures) {
            try {
                Map.Entry<T, R> entry = future.get();
                result.put(entry.getKey(), entry.getValue());
            } catch (Exception e) {
                throw new Exception("并行查询子任务失败 [" + taskName + "]: " + e.getMessage(), e);
            }
        }

        long cost = System.currentTimeMillis() - start;
        log.info("[ReportParallel] " + taskName + " 并行完成，"
                + total + " 个任务，耗时 " + cost + "ms");

        return result;
    }
}
