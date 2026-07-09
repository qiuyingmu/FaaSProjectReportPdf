package com.alibaba.work.faas.config;

import com.alibaba.fastjson.parser.ParserConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import javax.annotation.PostConstruct;

/**
 * 报表定时任务配置 + 全局安全配置。
 *
 * <p>配置调度线程池，避免定时任务阻塞 Spring Boot 的默认线程池。
 * 初始化 FastJSON 安全模式，防止反序列化攻击。</p>
 *
 * @author Senior Developer
 * 创建于 2026/07/07
 */
@Configuration
public class ReportConfig {

    private static final Logger log = LoggerFactory.getLogger(ReportConfig.class);

    @PostConstruct
    public void init() {
        // 开启 FastJSON safeMode，禁用 autoType 反序列化
        ParserConfig.getGlobalInstance().setSafeMode(true);
        log.info("FastJSON safeMode 已启用（autoType disabled）");
    }

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
