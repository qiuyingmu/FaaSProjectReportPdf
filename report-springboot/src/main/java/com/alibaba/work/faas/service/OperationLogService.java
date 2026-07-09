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

    private final OperationLogRepository repository;

    public OperationLogService(OperationLogRepository repository) {
        this.repository = repository;
    }

    /**
     * 异步记录操作日志。
     */
    @Async
    public void log(String operator, String action, String detail, String result, Long durationMs) {
        try {
            repository.save(new OperationLog(operator, action, detail, result, durationMs));
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
}
