package com.alibaba.work.faas.controller;

import com.alibaba.work.faas.entity.ApiKey;
import com.alibaba.work.faas.service.ApiKeyService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * API Key 管理接口（管理员）。
 */
@RestController
@RequestMapping("/api/admin/api-keys")
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    public ApiKeyController(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    /** 列出所有 Key（不含明文哈希） */
    @GetMapping
    public List<ApiKey> list() {
        return apiKeyService.list();
    }

    /**
     * 创建新 Key。
     * 请求体: { "name": "宜搭审批连接器" }
     * 响应包含明文 Key（仅此一次返回！）
     */
    @PostMapping
    public ApiKeyService.CreateResult create(@RequestBody Map<String, String> body) {
        String name = body.get("name");
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name 不能为空");
        }
        return apiKeyService.create(name.trim());
    }

    /** 启用 / 禁用 */
    @PutMapping("/{id}/enabled")
    public ApiKey setEnabled(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        Boolean enabled = body.get("enabled");
        if (enabled == null) {
            throw new IllegalArgumentException("enabled 不能为空");
        }
        return apiKeyService.setEnabled(id, enabled);
    }

    /** 删除 */
    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        apiKeyService.delete(id);
        return Map.of("success", true);
    }
}