package com.alibaba.work.faas.controller;

import com.alibaba.work.faas.model.entity.OperationLog;
import com.alibaba.work.faas.service.OperationLogService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

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
     * 获取最近的操作日志。
     */
    @GetMapping
    public Map<String, Object> getLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Page<OperationLog> p = operationLogService.getLogs(page, Math.min(size, 200));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("total", p.getTotalElements());
        result.put("logs", p.getContent());
        return result;
    }
}
