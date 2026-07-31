package com.alibaba.work.faas.util;

import javax.servlet.http.HttpServletRequest;

/**
 * 客户端 IP 解析工具。
 *
 * <p>服务部署在 Nginx 反向代理之后，真实 IP 需从 X-Forwarded-For 取；
 * 支持多级代理链（取第一个非 unknown 的地址）。</p>
 *
 * @author Senior Developer
 * 创建于 2026/07/31
 */
public final class ClientIpUtil {

    private ClientIpUtil() {}

    /** 常见可伪造/占位值 */
    private static final String UNKNOWN = "unknown";

    /**
     * 解析客户端真实 IP。
     * <p>优先级：X-Forwarded-For（取第一个有效）→ X-Real-IP → remoteAddr。</p>
     */
    public static String resolve(HttpServletRequest request) {
        if (request == null) {
            return "";
        }
        // 1. X-Forwarded-For：client, proxy1, proxy2... 取第一个非 unknown
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            for (String ip : forwarded.split(",")) {
                String trimmed = ip.trim();
                if (!trimmed.isEmpty() && !UNKNOWN.equalsIgnoreCase(trimmed)) {
                    return trimmed;
                }
            }
        }
        // 2. X-Real-IP（Nginx 配置了 proxy_set_header X-Real-IP）
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank() && !UNKNOWN.equalsIgnoreCase(realIp)) {
            return realIp.trim();
        }
        // 3. 直连
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "";
    }
}
