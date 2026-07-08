package com.alibaba.work.faas.model.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 定时任务配置实体。
 *
 * @author Senior Developer
 * 创建于 2026/07/08
 */
@Entity
@Table(name = "schedule_tasks")
public class ScheduleTaskEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_type", nullable = false, unique = true, length = 20)
    private String taskType;

    @Column(nullable = false, length = 100)
    private String cron;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "display_name", length = 50)
    private String displayName;

    @Column(length = 255)
    private String description;

    @Column(name = "time_range_code", length = 30)
    private String timeRangeCode;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    public ScheduleTaskEntity() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTaskType() { return taskType; }
    public void setTaskType(String taskType) { this.taskType = taskType; }
    public String getCron() { return cron; }
    public void setCron(String cron) { this.cron = cron; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getTimeRangeCode() { return timeRangeCode; }
    public void setTimeRangeCode(String timeRangeCode) { this.timeRangeCode = timeRangeCode; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
