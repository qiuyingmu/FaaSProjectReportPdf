package com.alibaba.work.faas.entity;

import javax.persistence.*;
import java.time.Instant;

/**
 * API Key 管理 —— 供外部系统（宜搭 HTTP 连接器等）调用我方接口的鉴权凭证。
 *
 * <p>Key 在创建时随机生成（如 <code>yida-a1b2c3d4...（64 字符）</code>），完整 Key 仅在创建时返回一次，
 * 之后只保存 BCrypt 哈希值用于验证。每个 Key 有独立的「名称」「启用状态」「最后使用时间」。</p>
 *
 * @author Senior Developer
 * 创建于 2026/07/29
 */
@Entity
@Table(name = "api_keys")
public class ApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 用户可读的 Key 名称 */
    @Column(nullable = false, length = 100)
    private String name;

    /** Key 的前缀（用于显示，类似 sk-abc...xyz），由生成时计算 */
    @Column(name = "key_prefix", nullable = false, length = 100)
    private String keyPrefix;

    /** Key 的 BCrypt 哈希值 */
    @Column(name = "key_hash", nullable = false, length = 255)
    private String keyHash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @Column(nullable = false)
    private boolean enabled = true;

    // ---- getters / setters ----
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getKeyPrefix() { return keyPrefix; }
    public void setKeyPrefix(String keyPrefix) { this.keyPrefix = keyPrefix; }

    public String getKeyHash() { return keyHash; }
    public void setKeyHash(String keyHash) { this.keyHash = keyHash; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getLastUsedAt() { return lastUsedAt; }
    public void setLastUsedAt(Instant lastUsedAt) { this.lastUsedAt = lastUsedAt; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}