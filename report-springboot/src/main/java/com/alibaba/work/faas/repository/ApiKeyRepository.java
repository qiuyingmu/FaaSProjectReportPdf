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

    /** 列出所有启用的 API Key（用于验证时扫描） */
    List<ApiKey> findAllByEnabledTrue();

    /**
     * 按 keyPrefix 前缀匹配启用的 Key（用于验证时快速定位候选）。
     * <p>keyPrefix 存储格式为「前8位...后4位」，传入完整 Key 的前 8 位即可
     * 通过 LIKE 缩小到 1~2 个候选，避免全量 BCrypt 遍历。</p>
     */
    List<ApiKey> findByEnabledTrueAndKeyPrefixStartingWith(String prefix);
}