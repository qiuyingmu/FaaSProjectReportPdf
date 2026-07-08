package com.alibaba.work.faas.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS 跨域配置 —— 允许 Vue 前端（localhost:3000）访问后端 API。
 *
 * @author Senior Developer
 * 创建于 2026/07/07
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        // 开发环境：允许本地前端调试；生产环境请修改为具体域名
        config.addAllowedOriginPattern("http://localhost:3000");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        source.registerCorsConfiguration("/actuator/**", config);
        source.registerCorsConfiguration("/admin/**", config);
        return new CorsFilter(source);
    }
}
