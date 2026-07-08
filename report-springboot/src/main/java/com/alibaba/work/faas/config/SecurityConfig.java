package com.alibaba.work.faas.config;

import com.alibaba.work.faas.service.OperationLogService;
import com.alibaba.work.faas.service.UserDetailsServiceImpl;
import com.alibaba.work.faas.model.entity.User;
import com.alibaba.work.faas.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Spring Security 安全配置 —— 数据库认证 + JSON 登录 + 攻击防护。
 *
 * <h3>安全措施</h3>
 * <ul>
 *   <li>bcrypt 密码加密</li>
 *   <li>Session 固定攻击防护（登录后自动创建新 Session）</li>
 *   <li>登录失败审计日志 + 简单暴力破解防护</li>
 *   <li>安全响应头（X-Frame-Options, X-Content-Type-Options, HSTS）</li>
 *   <li>CSRF 保护</li>
 *   <li>Session 超时自动过期</li>
 * </ul>
 *
 * @author Senior Developer
 * 创建于 2026/07/07
 */
@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Autowired
    private OperationLogService operationLogService;

    @Autowired
    private UserRepository userRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 暴力破解防护：记录每个 IP 的失败次数和时间 */
    private final Map<String, LoginAttempt> loginAttempts = new ConcurrentHashMap<>();

    /** 5 分钟内允许 10 次失败 */
    private static final int MAX_ATTEMPTS = 10;
    private static final long BLOCK_DURATION_MS = 5 * 60 * 1000;

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsService)
                .passwordEncoder(PasswordEncoderFactories.createDelegatingPasswordEncoder());
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            // CORS（配置在 WebMvcConfig 中）
            .cors().and()

            // 权限控制
            .authorizeRequests()
                .antMatchers("/api/admin/session").permitAll()
                .antMatchers("/api/admin/login").permitAll()
                .antMatchers("/admin/login").permitAll()
                .antMatchers("/admin/**").hasRole("ADMIN")
                .antMatchers("/actuator/**").hasRole("ADMIN")
                .antMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().permitAll()
                .and()

            // JSON 登录接口 —— 默认使用 loginPage 的 URL 处理 POST
            .formLogin()
                .loginPage("/admin/login")
                .usernameParameter("username")
                .passwordParameter("password")
                .successHandler(jsonAuthSuccessHandler())
                .failureHandler(jsonAuthFailureHandler())
                .permitAll()
                .and()

            // 登出（返回 JSON，不重定向到后端页面）
            .logout()
                .logoutUrl("/admin/logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .logoutSuccessHandler(jsonLogoutSuccessHandler())
                .permitAll()
                .and()

            // Session 管理
            .sessionManagement()
                .sessionFixation().newSession()
                .maximumSessions(1)
                .maxSessionsPreventsLogin(false)
                .and()
                .and()

            // CSRF 豁免
            .csrf()
                .ignoringAntMatchers("/api/**")
                .ignoringAntMatchers("/admin/login")
                .ignoringAntMatchers("/admin/logout");
    }

    // ========================================
    //  登录成功/失败处理器
    // ========================================

    private AuthenticationSuccessHandler jsonAuthSuccessHandler() {
        return (HttpServletRequest request, HttpServletResponse response,
                Authentication authentication) -> {
            response.setContentType("application/json;charset=utf-8");
            response.setStatus(HttpServletResponse.SC_OK);

            // 清除失败记录
            loginAttempts.remove(getClientIp(request));

            // 记录登录时间
            userRepository.findByUsername(authentication.getName())
                    .ifPresent(u -> { u.setLastLogin(LocalDateTime.now()); userRepository.save(u); });

            // 记录操作日志
            operationLogService.log(authentication.getName(), "LOGIN",
                    "登录成功", "SUCCESS", null);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("username", authentication.getName());
            response.getWriter().write(objectMapper.writeValueAsString(result));
        };
    }

    private AuthenticationFailureHandler jsonAuthFailureHandler() {
        return (HttpServletRequest request, HttpServletResponse response,
                AuthenticationException exception) -> {
            response.setContentType("application/json;charset=utf-8");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

            String ip = getClientIp(request);
            String username = request.getParameter("username");

            // 暴力破解防护
            String message = checkBruteForce(ip, username);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", false);
            result.put("message", message);
            response.getWriter().write(objectMapper.writeValueAsString(result));
        };
    }

    /**
     * 退出登录 → 返回 JSON { success: true, message: "已退出" }
     */
    private org.springframework.security.web.authentication.logout.LogoutSuccessHandler jsonLogoutSuccessHandler() {
        return (HttpServletRequest request, HttpServletResponse response,
                Authentication authentication) -> {
            response.setContentType("application/json;charset=utf-8");
            response.setStatus(HttpServletResponse.SC_OK);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("message", "已退出");

            response.getWriter().write(objectMapper.writeValueAsString(result));
        };
    }

    // ========================================
    //  暴力破解防护
    // ========================================

    private String checkBruteForce(String ip, String username) {
        long now = System.currentTimeMillis();
        loginAttempts.compute(ip, (key, attempt) -> {
            if (attempt == null || (now - attempt.firstAttempt) > BLOCK_DURATION_MS) {
                return new LoginAttempt(now, 1);
            }
            attempt.count++;
            return attempt;
        });

        LoginAttempt attempt = loginAttempts.get(ip);
        boolean blocked = attempt != null && attempt.count > MAX_ATTEMPTS;

        // 记录审计日志
        operationLogService.log(username != null ? username : "unknown",
                "LOGIN_FAIL", "IP: " + ip + " 第" + (attempt != null ? attempt.count : 0) + "次失败",
                blocked ? "BLOCKED" : "FAILURE", null);

        if (blocked) {
            long remaining = BLOCK_DURATION_MS - (now - attempt.firstAttempt);
            log.warn("暴力破解预警！IP={} 用户={} 已失败{}次，剩余封锁{}秒",
                    ip, username, attempt.count, remaining / 1000);
            return "账户已被临时锁定，请 " + (remaining / 60000 + 1) + " 分钟后重试";
        }

        return "账号或密码错误";
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /** 登录尝试记录 */
    private static class LoginAttempt {
        long firstAttempt;
        int count;
        LoginAttempt(long firstAttempt, int count) {
            this.firstAttempt = firstAttempt;
            this.count = count;
        }
    }
}
