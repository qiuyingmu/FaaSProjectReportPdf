package com.alibaba.work.faas.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

/**
 * 审批记录查询服务 —— 调用钉钉 Yida 1.0 API（HTTP 直连，不用 SDK）。
 *
 * <p>宜搭 2.0 SDK 不支持审批记录查询，需要用 1.0 的 REST API：
 * GET /v1.0/yida/processes/operationRecords</p>
 *
 * @author Senior Developer
 * 创建于 2026/07/29
 */
@Service
public class ApprovalService {

    private static final Logger log = LoggerFactory.getLogger(ApprovalService.class);

    /** 钉钉开放平台域名 */
    private static final String DINGTALK_API_HOST = "https://api.dingtalk.com";

    /** HTTP 客户端（复用连接池） */
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(10))
            .build();

    private final YidaApiManager yidaApiManager;
    private final ApiKeyService apiKeyService;

    public ApprovalService(YidaApiManager yidaApiManager, ApiKeyService apiKeyService) {
        this.yidaApiManager = yidaApiManager;
        this.apiKeyService = apiKeyService;
    }

    /**
     * 验证 API Key —— 委托给 ApiKeyService（数据库 BCrypt 校验 + .env 回退）。
     */
    public boolean verifyApiKey(String apiKey) {
        return apiKeyService.verifyApiKey(apiKey);
    }

    /**
     * 查询审批记录（钉钉 Yida 1.0 API）。
     * <p>appType/systemToken/userId 自动从系统配置读取，仅需传 processInstanceId。</p>
     *
     * @param processInstanceId 审批流程实例 ID
     * @return 审批记录 JSON 字符串（由钉钉 API 直接返回）
     * @throws Exception 查询失败时抛出
     */
    public String queryApprovalRecords(String processInstanceId) throws Exception {
        // 0. 自动读取系统配置
        String appType = yidaApiManager.getProductionSystemAppType();
        String systemToken = yidaApiManager.getProductionSystemSystemToken();
        String userId = yidaApiManager.getDefaultUserId();
        // 1. 获取 access token
        String accessToken = yidaApiManager.getAccessToken();

        // 2. 构建请求 URL
        String url = DINGTALK_API_HOST + "/v1.0/yida/processes/operationRecords"
                + "?appType=" + encode(appType)
                + "&systemToken=" + encode(systemToken)
                + "&userId=" + encode(userId)
                + "&language=zh_CN"
                + "&processInstanceId=" + encode(processInstanceId);

        // 3. 发 HTTP 请求
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("x-acs-dingtalk-access-token", accessToken)
                .header("Content-Type", "application/json")
                .GET()
                .timeout(java.time.Duration.ofSeconds(15))
                .build();

        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        int status = response.statusCode();
        String body = response.body();

        if (status != 200) {
            log.error("[ApprovalService] 钉钉 API 返回错误: status={}, body={}", status, body);
            throw new RuntimeException("查询审批记录失败: HTTP " + status);
        }

        log.info("[ApprovalService] 查询审批记录成功, processInstanceId={}", processInstanceId);
        return body;
    }

    private static String encode(String value) {
        try {
            return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            return value;
        }
    }
}
