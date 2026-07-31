package com.alibaba.work.faas.controller;

import com.alibaba.work.faas.entity.ApiAccessLog;
import com.alibaba.work.faas.service.ApiAccessLogService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * API 调用明细日志查询接口（管理员）—— 审计外部 API 调用。
 */
@RestController
@RequestMapping("/api/admin/api-access-logs")
public class ApiAccessLogAdminController {

    private final ApiAccessLogService apiAccessLogService;

    public ApiAccessLogAdminController(ApiAccessLogService apiAccessLogService) {
        this.apiAccessLogService = apiAccessLogService;
    }

    /**
     * 分页查询调用明细。
     * GET /api/admin/api-access-logs?page=0&size=50&keyPrefix=yida-a1b2...
     */
    @GetMapping
    public Map<String, Object> query(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String keyPrefix) {
        Page<ApiAccessLog> p = apiAccessLogService.query(keyPrefix, page, size);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("total", p.getTotalElements());
        result.put("logs", p.getContent());
        return result;
    }
}
