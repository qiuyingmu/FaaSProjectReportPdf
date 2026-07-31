package com.alibaba.work.faas.config;

import com.alibaba.work.faas.interceptor.ApiAccessLogInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS 跨域配置 —— 通过环境变量 CORS_ALLOWED_ORIGINS 控制。
 *
 * <p>支持多个来源，用逗号分隔。默认允许本地开发。</p>
 * <p>同时注册 API 调用日志拦截器（自动记录外部 API 明细）。</p>
 *
 * @author Senior Developer
 * 创建于 2026/07/07
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${CORS_ALLOWED_ORIGINS:http://localhost:3000,http://localhost:5173}")
    private String allowedOrigins;

    private final ApiAccessLogInterceptor apiAccessLogInterceptor;

    public WebMvcConfig(ApiAccessLogInterceptor apiAccessLogInterceptor) {
        this.apiAccessLogInterceptor = apiAccessLogInterceptor;
    }

    /** 注册 API 调用明细日志拦截器 */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(apiAccessLogInterceptor)
                .addPathPatterns("/api/yida/**");
    }

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
        return new CorsFilter(source);
    }
}
