package com.alibaba.work.faas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 宜搭运营报告 - Spring Boot 应用入口。
 *
 * @author Senior Developer
 * 创建于 2026/07/07
 */
@SpringBootApplication
@EnableScheduling
@EnableAsync
public class ReportApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReportApplication.class, args);
    }
}
