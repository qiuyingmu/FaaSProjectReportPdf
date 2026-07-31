package com.alibaba.work.faas.service;

import com.alibaba.work.faas.entity.FormConfig;
import com.alibaba.work.faas.repository.FormConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 表单数据源配置服务 —— 可配置化版本。
 *
 * <p>提供数据库配置的数据源定义，替代旧代码中 ReportConstants.SOURCES 的硬编码。
 * 报表逻辑只需依赖此服务提供的配置列表，无需感知数据库实现。</p>
 *
 * @author Senior Developer
 * 创建于 2026/07/31
 */
@Service
public class FormConfigService {

    private static final Logger log = LoggerFactory.getLogger(FormConfigService.class);

    private final FormConfigRepository repository;

    public FormConfigService(FormConfigRepository repository) {
        this.repository = repository;
    }

    /** 获取所有启用的表单配置（按 sortOrder 升序） */
    public List<FormConfig> getEnabledForms() {
        return repository.findAllByEnabledTrueOrderBySortOrderAsc();
    }

    /** 获取全部配置（含禁用，管理页用） */
    public List<FormConfig> listAll() {
        return repository.findAllByOrderBySortOrderAsc();
    }

    /** 新增或更新表单配置 */
    @Transactional
    public FormConfig save(FormConfig config) {
        if (config.getConfigKey() == null || config.getConfigKey().isBlank()) {
            throw new IllegalArgumentException("configKey 不能为空");
        }
        if (config.getFormUuid() == null || config.getFormUuid().isBlank()) {
            throw new IllegalArgumentException("formUuid 不能为空");
        }
        return repository.save(config);
    }

    /** 批量初始化（首次部署时把旧代码的硬编码配置写入数据库） */
    @Transactional
    public void seedDefaults(List<FormConfig> defaults) {
        for (FormConfig def : defaults) {
            if (repository.findByConfigKey(def.getConfigKey()).isEmpty()) {
                repository.save(def);
                log.info("[FormConfigService] 初始化表单配置: key={}, label={}",
                        def.getConfigKey(), def.getLabel());
            }
        }
    }

    /** 删除配置 */
    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }
}
