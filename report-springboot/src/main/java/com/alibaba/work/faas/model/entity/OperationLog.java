package com.alibaba.work.faas.model.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 操作日志实体。
 *
 * @author Senior Developer
 * 创建于 2026/07/08
 */
@Entity
@Table(name = "operation_logs", indexes = {
    @Index(name = "idx_ol_created_at", columnList = "created_at DESC"),
    @Index(name = "idx_ol_action_created", columnList = "action, created_at DESC"),
    @Index(name = "idx_ol_operator_created", columnList = "operator, created_at DESC")
})
public class OperationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 操作人 */
    @Column(name = "operator", length = 50)
    private String operator;

    /** 操作来源 IP（经 X-Forwarded-For 解析） */
    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    /** 操作类型（LOGIN / REPORT_GEN / SCHEDULE_UPDATE / EXPORT / ERROR） */
    @Column(name = "action", length = 50, nullable = false)
    private String action;

    /** 操作详情 */
    @Column(name = "detail", columnDefinition = "TEXT")
    private String detail;

    /** 执行结果（SUCCESS / FAILURE） */
    @Column(name = "result", length = 20)
    private String result;

    /** 耗时（毫秒） */
    @Column(name = "duration_ms")
    private Long durationMs;

    /** 操作时间 */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public OperationLog() {}

    public OperationLog(String operator, String action, String detail, String result, Long durationMs) {
        this(operator, null, action, detail, result, durationMs);
    }

    public OperationLog(String operator, String ipAddress, String action, String detail,
                        String result, Long durationMs) {
        this.operator = operator;
        this.ipAddress = ipAddress;
        this.action = action;
        this.detail = detail;
        this.result = result;
        this.durationMs = durationMs;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
