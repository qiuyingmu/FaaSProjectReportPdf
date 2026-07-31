package com.alibaba.work.faas.service;

import com.alibaba.work.faas.model.entity.OperationLog;
import com.alibaba.work.faas.repository.OperationLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    private final OperationLogRepository repository;

    public OperationLogService(OperationLogRepository repository) {
        this.repository = repository;
    }

    /**
     * 异步记录操作日志（无 IP，兼容旧调用）。
     */
    @Async
    public void log(String operator, String action, String detail, String result, Long durationMs) {
        log(operator, null, action, detail, result, durationMs);
    }

    /**
     * 异步记录操作日志（含来源 IP）。
     */
    @Async
    public void log(String operator, String ipAddress, String action, String detail,
                    String result, Long durationMs) {
        try {
            repository.save(new OperationLog(operator, ipAddress, action, detail, result, durationMs));
        } catch (Exception e) {
            log.warn("记录操作日志失败", e);
        }
    }

    /**
     * 获取最近的操作日志。
     */
    public List<OperationLog> getRecentLogs(int limit) {
        return repository.findTop100ByOrderByCreatedAtDesc();
    }

    /**
     * 分页查询操作日志（无筛选）。
     */
    public Page<OperationLog> getLogs(int page, int size) {
        return repository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size));
    }

    /**
     * 带筛选条件的分页查询操作日志。
     *
     * @param operator  操作人（模糊匹配）
     * @param action    操作类型（精确匹配）
     * @param result    结果（精确匹配）
     * @param startDate 起始时间（含）
     * @param endDate   截止时间（含）
     */
    public Page<OperationLog> searchLogs(String operator, String action, String result,
                                          LocalDateTime startDate, LocalDateTime endDate,
                                          int page, int size) {
        Specification<OperationLog> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(operator)) {
                predicates.add(cb.like(root.get("operator"), "%" + operator.trim() + "%"));
            }
            if (StringUtils.hasText(action)) {
                predicates.add(cb.equal(root.get("action"), action.trim()));
            }
            if (StringUtils.hasText(result)) {
                predicates.add(cb.equal(root.get("result"), result.trim()));
            }
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), endDate));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return repository.findAll(spec,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    /**
     * 操作日志报表统计（按操作类型 + 按天趋势）。
     *
     * @return Map 含 actionStats（按类型计数）和 dayStats（按天趋势）
     */
    public Map<String, Object> stats(LocalDateTime start, LocalDateTime end) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("actionStats", repository.countByAction(start, end));
        result.put("dayStats", repository.countByDay(start, end));
        return result;
    }
}
