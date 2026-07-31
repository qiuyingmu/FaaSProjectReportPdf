package com.alibaba.work.faas.entity;

import javax.persistence.*;

/**
 * 宜搭应用配置 —— 可配置化版本。
 *
 * <p>对应旧代码 YidaApiManager 中硬编码的 appType/systemToken/userId。
 * 通过数据库配置，应用更换或新增时无需改代码。</p>
 *
 * @author Senior Developer
 * 创建于 2026/07/31
 */
@Entity
@Table(name = "app_configs",
        uniqueConstraints = @UniqueConstraint(columnNames = "config_key"))
public class AppConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 配置唯一标识（如 production、testing） */
    @Column(name = "config_key", nullable = false, length = 50)
    private String configKey;

    /** 宜搭应用编码（APP_ 开头） */
    @Column(name = "app_type", nullable = false, length = 100)
    private String appType;

    /** 宜搭系统 Token */
    @Column(name = "system_token", nullable = false, length = 255)
    private String systemToken;

    /** 默认操作人钉钉 userId */
    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    /** 是否启用（当前生效的配置） */
    @Column(nullable = false)
    private boolean enabled = true;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getConfigKey() { return configKey; }
    public void setConfigKey(String configKey) { this.configKey = configKey; }

    public String getAppType() { return appType; }
    public void setAppType(String appType) { this.appType = appType; }

    public String getSystemToken() { return systemToken; }
    public void setSystemToken(String systemToken) { this.systemToken = systemToken; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
