package com.alibaba.work.faas.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Session 状态检查 API（无需登录即可访问）。
 *
 * <p>前端路由守卫通过此接口判断当前 Session 是否有效，
 * 避免 Spring Security 的 302 重定向导致前端无法正确处理。</p>
 *
 * @author Senior Developer
 * 创建于 2026/07/08
 */
@RestController
@RequestMapping("/api/admin")
public class SessionController {

    @GetMapping("/session")
    public Map<String, Object> checkSession() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> result = new LinkedHashMap<>();

        boolean valid = auth != null && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getPrincipal());

        result.put("valid", valid);
        if (valid) {
            result.put("username", auth.getName());
        }
        return result;
    }
}
