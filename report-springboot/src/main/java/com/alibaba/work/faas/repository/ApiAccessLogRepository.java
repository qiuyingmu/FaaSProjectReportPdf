package com.alibaba.work.faas.repository;

import com.alibaba.work.faas.entity.ApiAccessLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * API 调用明细日志 DAO。
 */
public interface ApiAccessLogRepository extends JpaRepository<ApiAccessLog, Long> {

    /** 按 Key 前缀分页查询调用明细（新→旧） */
    Page<ApiAccessLog> findByKeyPrefixOrderByCreatedAtDesc(String keyPrefix, Pageable pageable);

    /** 全部分页查询（新→旧） */
    Page<ApiAccessLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /** 删除指定时间之前的明细（保留策略） */
    @Modifying
    @Transactional
    @Query("DELETE FROM ApiAccessLog a WHERE a.createdAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") LocalDateTime cutoff);
}
