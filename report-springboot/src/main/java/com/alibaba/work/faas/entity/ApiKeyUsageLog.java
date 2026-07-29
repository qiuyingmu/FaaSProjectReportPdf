package com.alibaba.work.faas.entity;

import javax.persistence.*;
import java.time.LocalDate;

/**
 * API Key 调用统计 —— 按天聚合每个 Key 的请求次数。
 *
 * <p>用于在管理后台展示使用趋势图（类似 DeepSeek）。</p>
 */
@Entity
@Table(name = "api_key_usage_log",
        uniqueConstraints = @UniqueConstraint(columnNames = {"api_key_id", "usage_date"}))
public class ApiKeyUsageLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "api_key_id", nullable = false)
    private Long apiKeyId;

    @Column(name = "usage_date", nullable = false)
    private LocalDate usageDate;

    @Column(nullable = false)
    private long count = 0;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getApiKeyId() { return apiKeyId; }
    public void setApiKeyId(Long apiKeyId) { this.apiKeyId = apiKeyId; }

    public LocalDate getUsageDate() { return usageDate; }
    public void setUsageDate(LocalDate usageDate) { this.usageDate = usageDate; }

    public long getCount() { return count; }
    public void setCount(long count) { this.count = count; }
}