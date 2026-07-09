package com.alibaba.work.faas.controller;

import com.alibaba.work.faas.model.entity.OperationLog;
import com.alibaba.work.faas.service.OperationLogService;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 操作日志 API。
 *
 * @author Senior Developer
 * 创建于 2026/07/08
 */
@RestController
@RequestMapping("/api/admin/operations")
public class OperationLogController {

    private final OperationLogService operationLogService;

    public OperationLogController(OperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    /**
     * 查询操作日志（支持筛选和分页）。
     *
     * @param operator  操作人（模糊匹配）
     * @param action    操作类型（精确匹配）
     * @param result    结果（精确匹配）
     * @param startDate 起始时间（ISO 格式，如 2026-07-01T00:00:00）
     * @param endDate   截止时间（ISO 格式，如 2026-07-09T23:59:59）
     */
    @GetMapping
    public Map<String, Object> getLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String operator,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String result,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime endDate) {

        Page<OperationLog> p = operationLogService.searchLogs(
                operator, action, result, startDate, endDate,
                page, Math.min(size, 200));
        Map<String, Object> resultMap = new LinkedHashMap<>();
        resultMap.put("success", true);
        resultMap.put("total", p.getTotalElements());
        resultMap.put("logs", p.getContent());
        return resultMap;
    }
}
