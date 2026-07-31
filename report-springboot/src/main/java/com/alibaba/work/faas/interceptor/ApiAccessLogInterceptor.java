package com.alibaba.work.faas.interceptor;

import com.alibaba.work.faas.entity.ApiAccessLog;
import com.alibaba.work.faas.service.ApiAccessLogService;
import com.alibaba.work.faas.util.ClientIpUtil;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * API 调用明细日志拦截器 —— 自动记录外部 API 调用（含 IP、方法、路径、状态、耗时）。
 *
 * <p>只拦截 {@code /api/yida/**}（外部连接器入口），管理后台内部调用不记录，
 * 避免把管理员的操作也混进 API 审计日志。</p>
 *
 * @author Senior Developer
 * 创建于 2026/07/31
 */
@Component
public class ApiAccessLogInterceptor implements HandlerInterceptor {

    private final ApiAccessLogService apiAccessLogService;

    public ApiAccessLogInterceptor(ApiAccessLogService apiAccessLogService) {
        this.apiAccessLogService = apiAccessLogService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 记录开始时间到 request attribute
        request.setAttribute("_accessLogStart", System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        // 仅记录外部 API 入口（yida 连接器回调）
        String path = request.getRequestURI();
        if (!path.startsWith("/api/yida/")) {
            return;
        }

        long start = request.getAttribute("_accessLogStart") instanceof Long l ? l : System.currentTimeMillis();
        long cost = System.currentTimeMillis() - start;

        ApiAccessLog entry = new ApiAccessLog();
        entry.setKeyPrefix(extractKeyPrefix(request));
        entry.setSource("EXTERNAL");
        entry.setIp(ClientIpUtil.resolve(request));
        entry.setMethod(request.getMethod());
        entry.setPath(path);
        entry.setStatus(response.getStatus());
        entry.setDurationMs(cost);
        apiAccessLogService.record(entry);
    }

    /** 提取 Key 前缀（完整 Key 不落库） */
    private static String extractKeyPrefix(HttpServletRequest request) {
        String key = request.getHeader("token");
        if (key == null || key.isBlank()) {
            key = request.getHeader("X-API-Key");
        }
        if (key == null || key.isBlank()) {
            key = request.getParameter("token");
        }
        if (key == null || key.isBlank()) {
            key = request.getParameter("apiKey");
        }
        if (key == null || key.isBlank()) {
            return "unknown";
        }
        return key.length() > 16 ? key.substring(0, 16) + "..." : key;
    }
}
