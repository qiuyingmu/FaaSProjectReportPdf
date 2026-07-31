package com.alibaba.work.faas.controller;

import com.alibaba.work.faas.entity.FormConfig;
import com.alibaba.work.faas.service.FormConfigService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 表单数据源配置管理接口（管理员）—— 可配置化版本。
 */
@RestController
@RequestMapping("/api/admin/form-configs")
public class FormConfigAdminController {

    private final FormConfigService formConfigService;

    public FormConfigAdminController(FormConfigService formConfigService) {
        this.formConfigService = formConfigService;
    }

    /** 列出全部（含禁用） */
    @GetMapping
    public List<FormConfig> list() {
        return formConfigService.listAll();
    }

    /** 新增或更新 */
    @PostMapping
    public FormConfig save(@RequestBody FormConfig config) {
        return formConfigService.save(config);
    }

    /** 删除 */
    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        formConfigService.delete(id);
        return Map.of("success", true);
    }
}
