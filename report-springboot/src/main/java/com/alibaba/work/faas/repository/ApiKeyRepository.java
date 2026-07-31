package com.alibaba.work.faas.repository;

import com.alibaba.work.faas.entity.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * API Key 数据访问层。
 */
public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {

    /** 列出所有 API Key（按创建时间倒序） */
    List<ApiKey> findAllByOrderByCreatedAtDesc();

    /** 列出所有启用的 API Key（用于验证时扫描 + 内存前缀过滤） */
    List<ApiKey> findAllByEnabledTrue();
}