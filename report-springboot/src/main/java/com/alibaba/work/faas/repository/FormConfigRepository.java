package com.alibaba.work.faas.repository;

import com.alibaba.work.faas.entity.FormConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 表单数据源配置 DAO。
 */
public interface FormConfigRepository extends JpaRepository<FormConfig, Long> {

    /** 按排序序号列出所有启用的表单配置 */
    List<FormConfig> findAllByEnabledTrueOrderBySortOrderAsc();

    /** 列出所有配置（含禁用，管理页用） */
    List<FormConfig> findAllByOrderBySortOrderAsc();

    /** 按唯一标识查询 */
    Optional<FormConfig> findByConfigKey(String configKey);
}
