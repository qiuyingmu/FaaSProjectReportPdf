package com.alibaba.work.faas.schedule;

/**
 * 定时任务配置模型。
 *
 * <p>表示一个可动态配置的定时任务：任务类型、Cron 表达式、是否启用。</p>
 *
 * @author Senior Developer
 * 创建于 2026/07/07
 */
public class ScheduleTask {

    /** 任务类型标识（唯一） */
    private String type;

    /** Cron 表达式 */
    private String cron;

    /** 时间范围编码（lastWeek / lastMonth / lastQuarter / 自定义） */
    private String timeRangeCode;

    /** 是否启用 */
    private boolean enabled;

    /** 显示名称 */
    private String displayName;

    /** 说明 */
    private String description;

    public ScheduleTask() {}

    public ScheduleTask(String type, String cron, String timeRangeCode, boolean enabled,
                         String displayName, String description) {
        this.type = type;
        this.cron = cron;
        this.timeRangeCode = timeRangeCode;
        this.enabled = enabled;
        this.displayName = displayName;
        this.description = description;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getCron() { return cron; }
    public void setCron(String cron) { this.cron = cron; }
    public String getTimeRangeCode() { return timeRangeCode; }
    public void setTimeRangeCode(String timeRangeCode) { this.timeRangeCode = timeRangeCode; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
