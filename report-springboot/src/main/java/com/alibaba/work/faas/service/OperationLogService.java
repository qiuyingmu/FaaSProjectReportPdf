package com.alibaba.work.faas.service;

import com.alibaba.work.faas.model.entity.OperationLog;
import com.alibaba.work.faas.repository.OperationLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 操作日志服务 —— 记录和查询所有重要操作。
 *
 * <p>异步写入，避免影响主业务流程性能。</p>
 *
 * @author Senior Developer
 * 创建于 2026/07/08
 */
@Service
public class OperationLogService {

    private static final Logger log = LoggerFactory.getLogger(OperationLogService.class);

    @Autowired
    private OperationLogRepository repository;

    /**
     * 异步记录操作日志。
     */
    @Async
    public void log(String operator, String action, String detail, String result, Long durationMs) {
        try {
            repository.save(new OperationLog(operator, action, detail, result, durationMs));
        } catch (Exception e) {
            log.warn("记录操作日志失败: {}", e.getMessage());
        }
    }

    /**
     * 获取最近的操作日志。
     */
    public List<OperationLog> getRecentLogs(int limit) {
        return repository.findTop100ByOrderByCreatedAtDesc();
    }

    /**
     * 分页查询操作日志。
     */
    public Page<OperationLog> getLogs(int page, int size) {
        return repository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size));
    }
}
