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
 * <p><b>支持的请求方式</b>：</p>
 * <ul>
 *   <li>请求方法：GET（推荐）或 POST</li>
 *   <li>processInstanceId 通过 Query 参数或 Body 传递</li>
 *   <li>API Key 通过以下任一头/参数传递：
 *     <ul>
 *       <li>Header: X-API-Key</li>
 *       <li>Header: Api-Key</li>
 *       <li>Header: token</li>
 *       <li>Header: Authorization: Bearer &lt;key&gt;</li>
 *       <li>Query: apiKey=xxx 或 token=xxx</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * @author Senior Developer
 * 创建于 2026/07/29
 */
@RestController
@RequestMapping("/api/yida")
public class ApprovalController {

    private static final Logger log = LoggerFactory.getLogger(ApprovalController.class);

    private final ApprovalService approvalService;

    public ApprovalController(ApprovalService approvalService) {
        this.approvalService = approvalService;
    }

    /** GET 和 POST 都支持 */
    @RequestMapping(value = "/approval/records",
            method = { RequestMethod.GET, RequestMethod.POST })
    public ResponseEntity<?> queryApprovalRecords(
            @RequestHeader(value = "X-API-Key", required = false) String xApiKey,
            @RequestHeader(value = "Api-Key", required = false) String apiKeyHeader,
            @RequestHeader(value = "token", required = false) String tokenHeader,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(value = "processInstanceId", required = false) String processInstanceIdParam,
            @RequestParam(value = "apiKey", required = false) String apiKeyQuery,
            @RequestParam(value = "token", required = false) String tokenQuery,
            @RequestBody(required = false) Map<String, String> body) {

        // ---- 1. 提取 API Key（兼容多种传递方式） ----
        String apiKey = firstNonBlank(xApiKey, apiKeyHeader, tokenHeader, apiKeyQuery, tokenQuery);
        if (apiKey == null && authorization != null && authorization.startsWith("Bearer ")) {
            apiKey = authorization.substring(7).trim();
        }

        // ---- 2. 鉴权 ----
        if (apiKey == null || !approvalService.verifyApiKey(apiKey)) {
            log.warn("[ApprovalController] API Key 无效或缺失");
            return ResponseEntity.status(401)
                    .body(Map.of("success", false, "message", "API Key 无效"));
        }

        // ---- 3. 提取 processInstanceId（兼容 Query 参数和 Body） ----
        String processInstanceId = processInstanceIdParam;
        if (isEmpty(processInstanceId) && body != null) {
            processInstanceId = body.get("processInstanceId");
        }

        if (isEmpty(processInstanceId)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "需要 processInstanceId"));
        }

        // ---- 4. 查询审批记录 ----
        try {
            String result = approvalService.queryApprovalRecords(processInstanceId);
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

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) return v;
        }
        return null;
    }
}