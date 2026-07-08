package com.alibaba.work.faas.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * 报表定时任务配置。
 *
 * <p>配置调度线程池，避免定时任务阻塞 Spring Boot 的默认线程池。</p>
 *
 * @author Senior Developer
 * 创建于 2026/07/07
 */
@Configuration
public class ReportConfig {

    /**
     * 报告生成调度线程池。
     * 使用独立的线程池避免长时间运行的任务阻塞 Web 请求处理。
     */
    @Bean
    public TaskScheduler reportTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(3);
        scheduler.setThreadNamePrefix("report-schedule-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(60);
        return scheduler;
    }
}
