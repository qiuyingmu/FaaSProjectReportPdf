package com.alibaba.work.faas.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS 跨域配置 —— 通过环境变量 CORS_ALLOWED_ORIGINS 控制。
 *
 * <p>支持多个来源，用逗号分隔。默认允许本地开发。</p>
 *
 * @author Senior Developer
 * 创建于 2026/07/07
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${CORS_ALLOWED_ORIGINS:http://localhost:3000,http://localhost:5173}")
    private String allowedOrigins;

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);

        // 解析逗号分隔的域名列表
        for (String origin : allowedOrigins.split(",")) {
            String trimmed = origin.trim();
            if (StringUtils.hasText(trimmed)) {
                config.addAllowedOriginPattern(trimmed);
            }
        }

        config.addAllowedHeader("*");
        config.addAllowedMethod("*");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        source.registerCorsConfiguration("/actuator/**", config);
        source.registerCorsConfiguration("/admin/**", config);
        return new CorsFilter(source);
    }
}
