package com.alibaba.work.faas.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * API 调用明细日志 —— 审计用（谁、何时、哪个 IP、调了什么、结果如何）。
 *
 * <p>由 {@code ApiAccessLogInterceptor} 自动记录，Controller 无需手动打点。
 * 与 {@link ApiKeyUsageLog}（聚合统计）职责分离：明细审计 + 聚合图表。</p>
 *
 * @author Senior Developer
 * 创建于 2026/07/31
 */
@Entity
@Table(name = "api_access_logs", indexes = {
    @Index(name = "idx_aal_created", columnList = "createdAt DESC"),
    @Index(name = "idx_aal_key_created", columnList = "keyPrefix, createdAt DESC")
})
public class ApiAccessLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** API Key 前缀（如 yida-a1b2...），不存完整 Key，避免泄露 */
    @Column(name = "key_prefix", length = 50)
    private String keyPrefix;

    /** 调用方来源：EXTERNAL（宜搭连接器等） / INTERNAL（管理后台） */
    @Column(length = 20)
    private String source;

    /** 客户端 IP（经 X-Forwarded-For 解析） */
    @Column(length = 64)
    private String ip;

    /** 请求方法（GET/POST...） */
    @Column(length = 10)
    private String method;

    /** 请求路径 */
    @Column(length = 255)
    private String path;

    /** HTTP 状态码 */
    private Integer status;

    /** 耗时（毫秒） */
    @Column(name = "duration_ms")
    private Long durationMs;

    /** 调用时间 */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getKeyPrefix() { return keyPrefix; }
    public void setKeyPrefix(String keyPrefix) { this.keyPrefix = keyPrefix; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
