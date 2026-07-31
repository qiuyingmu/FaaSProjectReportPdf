package com.alibaba.work.faas.controller;

import com.alibaba.work.faas.entity.AppConfig;
import com.alibaba.work.faas.service.AppConfigService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 宜搭应用配置管理接口（管理员）—— 可配置化版本。
 */
@RestController
@RequestMapping("/api/admin/app-configs")
public class AppConfigAdminController {

    private final AppConfigService appConfigService;

    public AppConfigAdminController(AppConfigService appConfigService) {
        this.appConfigService = appConfigService;
    }

    /** 列出全部 */
    @GetMapping
    public List<AppConfig> list() {
        return appConfigService.listAll();
    }

    /** 新增或更新 */
    @PostMapping
    public AppConfig save(@RequestBody AppConfig config) {
        return appConfigService.save(config);
    }

    /** 删除 */
    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        appConfigService.delete(id);
        return Map.of("success", true);
    }
}
