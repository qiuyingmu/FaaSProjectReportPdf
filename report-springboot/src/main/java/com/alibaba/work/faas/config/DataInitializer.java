package com.alibaba.work.faas.config;

import com.alibaba.work.faas.model.entity.ScheduleTaskEntity;
import com.alibaba.work.faas.model.entity.User;
import com.alibaba.work.faas.repository.ScheduleTaskRepository;
import com.alibaba.work.faas.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.stereotype.Component;

/**
 * 数据库初始化 —— 首次启动时创建默认管理员和定时任务配置。
 *
 * @author Senior Developer
 * 创建于 2026/07/08
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ScheduleTaskRepository scheduleTaskRepository;

    @Value("${report.admin.username:admin}")
    private String adminUsername;

    @Value("${report.admin.password:admin123}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        // ---- 初始化管理员账号 ----
        if (!userRepository.existsByUsername(adminUsername)) {
            User admin = new User(adminUsername,
                    PasswordEncoderFactories.createDelegatingPasswordEncoder().encode(adminPassword));
            admin.setRole("ADMIN");
            userRepository.save(admin);
            log.info("✅ 默认管理员账号已创建: {}", adminUsername);
        } else {
            log.info("管理员账号已存在: {}", adminUsername);
        }

        // ---- 初始化定时任务配置 ----
        initTask("weekly",    "0 0 10 * * MON",       "lastWeek",    true,  "周报",  "每周一 10:00 生成上周报告");
        initTask("monthly",   "0 0 10 1 * ?",         "lastMonth",   true,  "月报",  "每月 1 日 10:00 生成上月报告");
        initTask("quarterly", "0 0 10 1 1,4,7,10 ?", "lastQuarter", true,  "季报",  "每季度首月 1 日 10:00 生成上季度报告");
    }

    private void initTask(String type, String cron, String timeRangeCode, boolean enabled,
                           String displayName, String description) {
        if (scheduleTaskRepository.findByTaskType(type).isPresent()) return;

        ScheduleTaskEntity task = new ScheduleTaskEntity();
        task.setTaskType(type);
        task.setCron(cron);
        task.setTimeRangeCode(timeRangeCode);
        task.setEnabled(enabled);
        task.setDisplayName(displayName);
        task.setDescription(description);
        scheduleTaskRepository.save(task);
        log.info("✅ 默认定时任务已创建: {} ({})", displayName, cron);
    }
}
