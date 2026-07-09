package com.alibaba.work.faas.repository;

import com.alibaba.work.faas.model.entity.OperationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 操作日志数据访问层。
 *
 * @author Senior Developer
 * 创建于 2026/07/08
 */
@Repository
public interface OperationLogRepository extends JpaRepository<OperationLog, Long>,
        JpaSpecificationExecutor<OperationLog> {
    Page<OperationLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
    List<OperationLog> findTop100ByOrderByCreatedAtDesc();
}
