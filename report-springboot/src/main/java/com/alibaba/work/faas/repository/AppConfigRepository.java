package com.alibaba.work.faas.repository;

import com.alibaba.work.faas.entity.AppConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 宜搭应用配置 DAO。
 */
public interface AppConfigRepository extends JpaRepository<AppConfig, Long> {

    /** 列出所有启用配置 */
    List<AppConfig> findAllByEnabledTrue();

    /** 按唯一标识查询 */
    Optional<AppConfig> findByConfigKey(String configKey);
}
