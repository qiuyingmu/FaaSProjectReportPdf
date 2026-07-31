package com.alibaba.work.faas.repository;

import com.alibaba.work.faas.model.entity.OperationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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

    /** 按操作类型聚合统计（指定时间范围） */
    @Query("SELECT o.action AS action, COUNT(o) AS cnt FROM OperationLog o " +
           "WHERE o.createdAt BETWEEN :start AND :end GROUP BY o.action ORDER BY cnt DESC")
    List<Map<String, Object>> countByAction(@Param("start") LocalDateTime start,
                                            @Param("end") LocalDateTime end);

    /** 按天聚合操作数（指定时间范围，用于趋势图） */
    @Query("SELECT FUNCTION('DATE', o.createdAt) AS day, COUNT(o) AS cnt " +
           "FROM OperationLog o WHERE o.createdAt BETWEEN :start AND :end " +
           "GROUP BY FUNCTION('DATE', o.createdAt) ORDER BY day ASC")
    List<Map<String, Object>> countByDay(@Param("start") LocalDateTime start,
                                         @Param("end") LocalDateTime end);
}
