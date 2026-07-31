package com.alibaba.work.faas.controller;

import com.alibaba.work.faas.entity.ApiKey;
import com.alibaba.work.faas.entity.ApiKeyUsageLog;
import com.alibaba.work.faas.service.ApiKeyService;
import com.alibaba.work.faas.service.OperationLogService;
import com.alibaba.work.faas.util.ClientIpUtil;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * API Key 管理接口（管理员）。
 *
 * <p>所有写操作（创建/启停/删除）均记录到操作日志审计表（含操作人 + IP）。</p>
 */
@RestController
@RequestMapping("/api/admin/api-keys")
public class ApiKeyController {

    private final ApiKeyService apiKeyService;
    private final OperationLogService operationLogService;

    public ApiKeyController(ApiKeyService apiKeyService, OperationLogService operationLogService) {
        this.apiKeyService = apiKeyService;
        this.operationLogService = operationLogService;
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
    public ApiKeyService.CreateResult create(@RequestBody Map<String, String> body,
                                             Authentication auth, HttpServletRequest request) {
        String name = body.get("name");
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name 不能为空");
        }
        String operator = auth != null ? auth.getName() : "unknown";
        ApiKeyService.CreateResult result = apiKeyService.create(name.trim());
        operationLogService.log(operator, ClientIpUtil.resolve(request), "API_KEY_CREATE",
                "创建 API Key: " + name.trim() + " (id=" + result.apiKey.getId() + ")",
                "SUCCESS", null);
        return result;
    }

    /** 启用 / 禁用 */
    @PutMapping("/{id}/enabled")
    public ApiKey setEnabled(@PathVariable Long id, @RequestBody Map<String, Boolean> body,
                             Authentication auth, HttpServletRequest request) {
        Boolean enabled = body.get("enabled");
        if (enabled == null) {
            throw new IllegalArgumentException("enabled 不能为空");
        }
        String operator = auth != null ? auth.getName() : "unknown";
        ApiKey key = apiKeyService.setEnabled(id, enabled);
        operationLogService.log(operator, ClientIpUtil.resolve(request), "API_KEY_TOGGLE",
                (enabled ? "启用" : "禁用") + " API Key: " + key.getName() + " (id=" + id + ")",
                "SUCCESS", null);
        return key;
    }

    /** 删除 */
    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id,
                                      Authentication auth, HttpServletRequest request) {
        String operator = auth != null ? auth.getName() : "unknown";
        apiKeyService.delete(id);
        operationLogService.log(operator, ClientIpUtil.resolve(request), "API_KEY_DELETE",
                "删除 API Key (id=" + id + ")", "SUCCESS", null);
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