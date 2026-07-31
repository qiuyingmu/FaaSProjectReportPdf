package com.alibaba.work.faas.entity;

import javax.persistence.*;

/**
 * 表单数据源配置 —— 可配置化版本。
 *
 * <p>对应旧代码 ReportConstants.SOURCES 中的单个数据源定义。
 * 通过数据库配置，无需修改代码即可增删数据源或调整字段。</p>
 *
 * @author Senior Developer
 * 创建于 2026/07/31
 */
@Entity
@Table(name = "form_configs",
        uniqueConstraints = @UniqueConstraint(columnNames = "config_key"))
public class FormConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 配置唯一标识（如 docLib、dynamic、log...），旧代码 SOURCES 的 key 一致 */
    @Column(name = "config_key", nullable = false, length = 50)
    private String configKey;

    /** 显示名称（如 资料库、监理日志） */
    @Column(nullable = false, length = 50)
    private String label;

    /** 图表颜色（如 #3b82f6） */
    @Column(length = 20)
    private String color;

    /** 宜搭表单 UUID */
    @Column(name = "form_uuid", nullable = false, length = 100)
    private String formUuid;

    /** 人员/项目关联字段 ID */
    @Column(name = "person_field", nullable = false, length = 100)
    private String personField;

    /** 日期筛选字段 ID */
    @Column(name = "date_field", nullable = false, length = 100)
    private String dateField;

    /** 是否启用（false 则报表中不展示） */
    @Column(nullable = false)
    private boolean enabled = true;

    /** 排序序号（小的在前） */
    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getConfigKey() { return configKey; }
    public void setConfigKey(String configKey) { this.configKey = configKey; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getFormUuid() { return formUuid; }
    public void setFormUuid(String formUuid) { this.formUuid = formUuid; }

    public String getPersonField() { return personField; }
    public void setPersonField(String personField) { this.personField = personField; }

    public String getDateField() { return dateField; }
    public void setDateField(String dateField) { this.dateField = dateField; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}
