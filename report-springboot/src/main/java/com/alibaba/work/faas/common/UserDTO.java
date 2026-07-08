package com.alibaba.work.faas.common;

import java.time.LocalDateTime;

/**
 * 管理员用户 DTO —— 不暴露密码哈希等敏感字段。
 *
 * @author Senior Developer
 * 创建于 2026/07/08
 */
public class UserDTO {
    private Long id;
    private String username;
    private String role;
    private boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime lastLogin;

    // 仅用于新建/修改时的密码输入，不会返回给前端
    private String password;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getLastLogin() { return lastLogin; }
    public void setLastLogin(LocalDateTime lastLogin) { this.lastLogin = lastLogin; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
