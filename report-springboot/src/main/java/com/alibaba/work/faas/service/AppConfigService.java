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

    public AppConfigService(AppConfigRepository repository) {
        this.repository = repository;
    }

    /**
     * 获取当前生效的应用配置。
     * 优先数据库；数据库为空返回 null（调用方回退 .env/硬编码）。
     * <p>注意：这里不做 .env 回退构造，避免与 YidaApiManager 的
     * getProductionSystemAppType() 形成无限递归（getter→getActiveConfig→getter）。</p>
     */
    public AppConfig getActiveConfig() {
        List<AppConfig> configs = repository.findAllByEnabledTrue();
        if (!configs.isEmpty()) {
            return configs.get(0);
        }
        return null;
    }

    /** 列出全部配置 */
    public List<AppConfig> listAll() {
        return repository.findAll();
    }

    /** 新增或更新（systemToken 留空表示不修改，防止前端编辑时误覆盖凭证） */
    @Transactional
    public AppConfig save(AppConfig config) {
        if (config.getConfigKey() == null || config.getConfigKey().isBlank()) {
            throw new IllegalArgumentException("configKey 不能为空");
        }
        if (config.getAppType() == null || config.getAppType().isBlank()) {
            throw new IllegalArgumentException("appType 不能为空");
        }
        if (config.getUserId() == null || config.getUserId().isBlank()) {
            throw new IllegalArgumentException("userId 不能为空");
        }
        // systemToken 为空 → 更新时保留原值（新增时必须提供）
        if (config.getSystemToken() == null || config.getSystemToken().isBlank()) {
            if (config.getId() != null) {
                AppConfig existing = repository.findById(config.getId()).orElse(null);
                if (existing != null) {
                    config.setSystemToken(existing.getSystemToken());
                }
            }
        }
        if (config.getSystemToken() == null || config.getSystemToken().isBlank()) {
            throw new IllegalArgumentException("systemToken 不能为空（新增时必须提供）");
        }
        return repository.save(config);
    }

    /** 删除 */
    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }
}
