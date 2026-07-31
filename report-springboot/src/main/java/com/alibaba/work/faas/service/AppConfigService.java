package com.alibaba.work.faas.service;

import com.alibaba.work.faas.entity.AppConfig;
import com.alibaba.work.faas.repository.AppConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 宜搭应用配置服务 —— 可配置化版本。
 *
 * <p>优先读取数据库配置（app_configs 表），表为空时回退到 YidaApiManager
 * 的既有硬编码/ .env 配置，保证不配置也能用。</p>
 *
 * @author Senior Developer
 * 创建于 2026/07/31
 */
@Service
public class AppConfigService {

    private static final Logger log = LoggerFactory.getLogger(AppConfigService.class);

    private final AppConfigRepository repository;
    private final YidaApiManager yidaApiManager;

    public AppConfigService(AppConfigRepository repository, YidaApiManager yidaApiManager) {
        this.repository = repository;
        this.yidaApiManager = yidaApiManager;
    }

    /**
     * 获取当前生效的应用配置。
     * 优先数据库；数据库为空时回退 YidaApiManager 既有配置（不报错）。
     */
    public AppConfig getActiveConfig() {
        List<AppConfig> configs = repository.findAllByEnabledTrue();
        if (!configs.isEmpty()) {
            return configs.get(0);
        }
        // 回退：用 YidaApiManager 既有配置构造（数据库无配置时保持旧行为）
        AppConfig fallback = new AppConfig();
        fallback.setConfigKey("fallback");
        fallback.setAppType(yidaApiManager.getProductionSystemAppType());
        fallback.setSystemToken(yidaApiManager.getProductionSystemSystemToken());
        fallback.setUserId(yidaApiManager.getDefaultUserId());
        fallback.setEnabled(true);
        return fallback;
    }

    /** 列出全部配置 */
    public List<AppConfig> listAll() {
        return repository.findAll();
    }

    /** 新增或更新 */
    @Transactional
    public AppConfig save(AppConfig config) {
        if (config.getConfigKey() == null || config.getConfigKey().isBlank()) {
            throw new IllegalArgumentException("configKey 不能为空");
        }
        if (config.getAppType() == null || config.getAppType().isBlank()) {
            throw new IllegalArgumentException("appType 不能为空");
        }
        return repository.save(config);
    }

    /** 删除 */
    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }
}
