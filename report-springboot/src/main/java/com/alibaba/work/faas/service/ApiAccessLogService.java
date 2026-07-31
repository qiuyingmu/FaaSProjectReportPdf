package com.alibaba.work.faas.service;

import com.alibaba.work.faas.entity.ApiAccessLog;
import com.alibaba.work.faas.repository.ApiAccessLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * API 调用明细日志服务 —— 异步写入 + 查询 + 定期清理。
 *
 * <p>保留策略：默认 90 天（可配置 {@code report.api-log.retention-days}），
 * 每天凌晨自动清理过期明细。</p>
 *
 * @author Senior Developer
 * 创建于 2026/07/31
 */
@Service
public class ApiAccessLogService {

    private static final Logger log = LoggerFactory.getLogger(ApiAccessLogService.class);

    /** 默认保留天数 */
    private static final int DEFAULT_RETENTION_DAYS = 90;

    private final ApiAccessLogRepository repository;

    public ApiAccessLogService(ApiAccessLogRepository repository) {
        this.repository = repository;
    }

    /** 异步记录一条调用明细（失败不影响主流程） */
    @Async
    public void record(ApiAccessLog entry) {
        try {
            repository.save(entry);
        } catch (Exception e) {
            log.warn("[ApiAccessLogService] 记录 API 调用日志失败: {}", e.getMessage());
        }
    }

    /** 分页查询（可按 Key 前缀过滤） */
    public Page<ApiAccessLog> query(String keyPrefix, int page, int size) {
        PageRequest pageable = PageRequest.of(page, Math.min(size, 200),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        if (keyPrefix != null && !keyPrefix.isBlank()) {
            return repository.findByKeyPrefixOrderByCreatedAtDesc(keyPrefix.trim(), pageable);
        }
        return repository.findAllByOrderByCreatedAtDesc(pageable);
    }

    /**
     * 每日凌晨清理过期明细（保留期之外删除）。
     * 首次启动时也会执行一次，避免历史脏数据堆积。
     */
    @Scheduled(cron = "0 10 3 * * ?")
    public void cleanup() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(DEFAULT_RETENTION_DAYS);
        try {
            int removed = repository.deleteOlderThan(cutoff);
            if (removed > 0) {
                log.info("[ApiAccessLogService] 清理 {} 条过期 API 调用日志（{} 天前）", removed, DEFAULT_RETENTION_DAYS);
            }
        } catch (Exception e) {
            log.warn("[ApiAccessLogService] 清理过期日志失败: {}", e.getMessage());
        }
    }
}
