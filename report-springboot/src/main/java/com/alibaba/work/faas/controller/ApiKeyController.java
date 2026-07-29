package com.alibaba.work.faas.controller;

import com.alibaba.work.faas.entity.ApiKey;
import com.alibaba.work.faas.entity.ApiKeyUsageLog;
import com.alibaba.work.faas.service.ApiKeyService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
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

    /**
     * 查询 Key 的每日调用统计。
     * GET /api/admin/api-keys/{id}/stats?from=2026-07-01&to=2026-07-29
     */
    @GetMapping("/{id}/stats")
    public Map<String, Object> stats(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        List<ApiKeyUsageLog> records = apiKeyService.getStats(id, from, to);
        // 填充没有数据的日期（保持图表连续）
        List<Map<String, Object>> daily = new java.util.ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            LocalDate date = d;
            long cnt = records.stream()
                    .filter(r -> r.getUsageDate().equals(date))
                    .mapToLong(ApiKeyUsageLog::getCount)
                    .sum();
            daily.add(Map.of("date", date.toString(), "count", cnt));
        }
        return Map.of("daily", daily);
    }
}