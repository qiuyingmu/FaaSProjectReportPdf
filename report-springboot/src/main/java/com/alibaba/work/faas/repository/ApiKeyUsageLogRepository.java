package com.alibaba.work.faas.repository;

import com.alibaba.work.faas.entity.ApiKeyUsageLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * API Key 调用统计 DAO。
 */
public interface ApiKeyUsageLogRepository extends JpaRepository<ApiKeyUsageLog, Long> {

    /** 单 Key 指定日期范围内统计（按日期升序） */
    @Query("SELECT u FROM ApiKeyUsageLog u WHERE u.apiKeyId = :keyId AND u.usageDate BETWEEN :from AND :to ORDER BY u.usageDate ASC")
    List<ApiKeyUsageLog> findRange(@Param("keyId") Long keyId,
                                   @Param("from") LocalDate from,
                                   @Param("to") LocalDate to);

    /**
     * 当日 +1：INSERT ON CONFLICT DO UPDATE（PostgreSQL 语法）。
     * 用单条 SQL 原子性更新，避免 SELECT-then-UPDATE 的并发问题。
     */
    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO api_key_usage_log (api_key_id, usage_date, count)
            VALUES (:keyId, :date, 1)
            ON CONFLICT (api_key_id, usage_date)
            DO UPDATE SET count = api_key_usage_log.count + 1
            """, nativeQuery = true)
    void incrementDaily(@Param("keyId") Long keyId, @Param("date") LocalDate date);

    /** 批量删除某 Key 的全部统计记录（删除 Key 时级联清理） */
    @Modifying
    @Transactional
    @Query("DELETE FROM ApiKeyUsageLog u WHERE u.apiKeyId = :keyId")
    void deleteByApiKeyId(@Param("keyId") Long keyId);
}