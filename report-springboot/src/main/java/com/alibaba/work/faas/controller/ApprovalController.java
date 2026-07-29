package com.alibaba.work.faas.controller;

import com.alibaba.work.faas.service.ApprovalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 审批记录查询接口 —— 供宜搭 HTTP 连接器回调。
 *
 * <p>宜搭集成自动化中配置 HTTP 连接器，回调此接口获取审批进度信息。
 * 接口使用 API Key 鉴权，需在 .env 中配置 yida.connector.api.key。</p>
 *
 * @author Senior Developer
 * 创建于 2026/07/29
 */
@RestController
@RequestMapping("/api/yida")
public class ApprovalController {

    private static final Logger log = LoggerFactory.getLogger(ApprovalController.class);

    /** 请求头：API Key */
    private static final String HEADER_API_KEY = "X-API-Key";

    private final ApprovalService approvalService;

    public ApprovalController(ApprovalService approvalService) {
        this.approvalService = approvalService;
    }

    /**
     * 查询审批记录。
     *
     * <p>宜搭 HTTP 连接器端配置：</p>
     * <ul>
     *   <li>URL: POST http://your-server/api/yida/approval/records</li>
     *   <li>Headers: X-API-Key = {.env 中配置的 yida.connector.api.key}</li>
     *   <li>Body (JSON):</li>
     * </ul>
     * <pre>
     * {
     *   "appType": "APP_PBKT0xxx",
     *   "systemToken": "hexxxx",
     *   "userId": "user123",
     *   "processInstanceId": "f30233fb-72e1-4af4-8cb8-c7e0ea9ee530"
     * }
     * </pre>
     */
    @PostMapping("/approval/records")
    public ResponseEntity<?> queryApprovalRecords(
            @RequestHeader(value = HEADER_API_KEY, required = false) String apiKey,
            @RequestBody(required = false) Map<String, String> body) {

        // ---- 1. 鉴权 ----
        if (apiKey == null || !approvalService.verifyApiKey(apiKey)) {
            log.warn("[ApprovalController] API Key 无效或缺失");
            return ResponseEntity.status(401)
                    .body(Map.of("success", false, "message", "API Key 无效"));
        }

        // ---- 2. 参数校验 ----
        if (body == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "请求体不能为空"));
        }

        String appType = body.get("appType");
        String systemToken = body.get("systemToken");
        String userId = body.get("userId");
        String processInstanceId = body.get("processInstanceId");

        if (isEmpty(appType) || isEmpty(systemToken) || isEmpty(userId) || isEmpty(processInstanceId)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false,
                            "message", "参数不完整，需要 appType/systemToken/userId/processInstanceId"));
        }

        // ---- 3. 查询审批记录 ----
        try {
            String result = approvalService.queryApprovalRecords(
                    appType, systemToken, userId, processInstanceId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("[ApprovalController] 查询审批记录异常: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "message", "查询失败: " + e.getMessage()));
        }
    }

    private static boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }
}
