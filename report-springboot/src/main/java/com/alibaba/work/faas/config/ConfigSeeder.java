package com.alibaba.work.faas.config;

import com.alibaba.work.faas.entity.AppConfig;
import com.alibaba.work.faas.entity.FormConfig;
import com.alibaba.work.faas.repository.AppConfigRepository;
import com.alibaba.work.faas.service.FormConfigService;
import com.alibaba.work.faas.service.YidaApiManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * 配置初始化器 —— 首次部署时把旧代码的硬编码表单配置写入数据库。
 *
 * <p>仅当配置缺失时写入默认值；已有数据则跳过（不会覆盖用户手动改过的配置）。
 * 应用凭证（appType/systemToken/userId）：表为空时从 .env / yida-secret.properties
 * 加载一条默认配置入库（管理后台可见可改），已有数据跳过。</p>
 *
 * @author Senior Developer
 * 创建于 2026/07/31
 */
@Component
public class ConfigSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ConfigSeeder.class);

    private final FormConfigService formConfigService;
    private final AppConfigRepository appConfigRepository;
    private final YidaApiManager yidaApiManager;

    public ConfigSeeder(FormConfigService formConfigService,
                        AppConfigRepository appConfigRepository,
                        YidaApiManager yidaApiManager) {
        this.formConfigService = formConfigService;
        this.appConfigRepository = appConfigRepository;
        this.yidaApiManager = yidaApiManager;
    }

    @Override
    public void run(String... args) {
        seedFormConfigs();
        seedAppConfig();
    }

    /** 与旧代码 ReportConstants.SOURCES 一致的默认表单配置 */
    private void seedFormConfigs() {
        List<FormConfig> defaults = Arrays.asList(
                form("docLib",  "资料库",      "#3b82f6", "FORM-4ADCB90E94A44F22BC14265B222AE6E54P9S", "textField_ml6no8vf", "dateField_ml6no8wf", 0),
                form("dynamic", "项目动态",    "#10b981", "FORM-7F2A51B894CC4F348DC5C2B840772F36RHM3", "textField_mlg9av31", "dateField_mlelkrk3", 1),
                form("log",     "监理日志",    "#f59e0b", "FORM-ACD427B1D88545F5BCC4B20DB6B4BD9FJVAX", "textField_mjjngk77", "dateField_mkat24th", 2),
                form("safeLog", "日志(安全)",  "#8b5cf6", "FORM-7FC88BAA352945F29E30B337BCFD49C97TBT", "textField_mjjngk77", "dateField_mkauls9o", 3),
                form("station", "旁站记录",    "#ef4444", "FORM-AF24B9440B5B41F1AC830065140D73FB1XBK", "textField_mj89asvv", "dateField_mjjitv2s", 4),
                form("hazard",  "安全隐患",    "#14b8a6", "FORM-04BDB63138D34DDE9A1330EEBD550473E4HJ", "textField_mj89asvv", "dateField_mnzhv2ul", 5)
        );
        formConfigService.seedDefaults(defaults);
        log.info("[ConfigSeeder] 表单配置初始化完成（缺省补入，已有跳过）");
    }

    private static FormConfig form(String key, String label, String color, String formUuid,
                                   String personField, String dateField, int sortOrder) {
        FormConfig c = new FormConfig();
        c.setConfigKey(key);
        c.setLabel(label);
        c.setColor(color);
        c.setFormUuid(formUuid);
        c.setPersonField(personField);
        c.setDateField(dateField);
        c.setEnabled(true);
        c.setSortOrder(sortOrder);
        return c;
    }

    /** 应用配置 seed：表为空时把 .env / yida-secret.properties 的凭证加载入库 */
    private void seedAppConfig() {
        if (appConfigRepository.count() > 0) {
            log.info("[ConfigSeeder] 应用配置已存在，跳过 seed");
            return;
        }
        AppConfig app = new AppConfig();
        app.setConfigKey("production");
        app.setAppType(yidaApiManager.getProductionSystemAppType());
        app.setSystemToken(yidaApiManager.getProductionSystemSystemToken());
        app.setUserId(yidaApiManager.getDefaultUserId());
        app.setEnabled(true);
        appConfigRepository.save(app);
        log.info("[ConfigSeeder] 应用配置已从 .env 加载到数据库: production (appType={})", app.getAppType());
    }
}
