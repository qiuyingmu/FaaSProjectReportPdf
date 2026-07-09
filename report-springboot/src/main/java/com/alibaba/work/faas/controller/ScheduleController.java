package com.alibaba.work.faas.controller;

import com.alibaba.work.faas.schedule.DynamicScheduler;
import com.alibaba.work.faas.schedule.ScheduleTask;
import com.alibaba.work.faas.service.OperationLogService;
import com.alibaba.work.faas.service.ScheduleTaskService;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 定时任务管理 API —— 支持新增/修改/启停/删除。
 *
 * @author Senior Developer
 * 创建于 2026/07/07
 */
@RestController
@RequestMapping("/api/admin/schedules")
public class ScheduleController {

    private final DynamicScheduler dynamicScheduler;
    private final ScheduleTaskService scheduleTaskService;
    private final OperationLogService operationLogService;

    public ScheduleController(DynamicScheduler dynamicScheduler,
                               ScheduleTaskService scheduleTaskService,
                               OperationLogService operationLogService) {
        this.dynamicScheduler = dynamicScheduler;
        this.scheduleTaskService = scheduleTaskService;
        this.operationLogService = operationLogService;
    }

    /** 获取所有任务 */
    @GetMapping
    public Map<String, Object> listSchedules() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("tasks", dynamicScheduler.getTasks());
        return result;
    }

    /** 新增定时任务 */
    @PostMapping(consumes = "application/json")
    public Map<String, Object> createSchedule(@RequestBody ScheduleTask task) {
        if (task.getType() == null || task.getType().isEmpty()) {
            return errorResult("任务类型不能为空");
        }
        if (task.getCron() == null || task.getCron().isEmpty()) {
            return errorResult("Cron 表达式不能为空");
        }
        if (scheduleTaskService.findByType(task.getType()) != null) {
            return errorResult("任务类型 '" + task.getType() + "' 已存在");
        }

        ScheduleTask saved = scheduleTaskService.save(task);
        dynamicScheduler.addTask(saved);
        operationLogService.log("admin", "SCHEDULE_CREATE",
                "新增任务 " + task.getType() + " cron=" + task.getCron(), "SUCCESS", null);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("task", saved);
        return result;
    }

    /** 更新定时任务 */
    @PutMapping(value = "/{type}", consumes = "application/json")
    public Map<String, Object> updateSchedule(
            @PathVariable String type,
            @RequestBody ScheduleTask task) {
        task.setType(type);
        ScheduleTask updated = dynamicScheduler.updateTask(task);
        operationLogService.log("admin", "SCHEDULE_UPDATE",
                "更新任务 " + type + " Cron: " + task.getCron(), "SUCCESS", null);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("task", updated);
        return result;
    }

    /** 启停任务 */
    @PostMapping("/{type}/toggle")
    public Map<String, Object> toggleSchedule(@PathVariable String type) {
        ScheduleTask task = dynamicScheduler.getTasks().stream()
                .filter(t -> t.getType().equals(type))
                .findFirst()
                .orElse(null);
        if (task == null) {
            return errorResult("任务不存在: " + type);
        }
        task.setEnabled(!task.isEnabled());
        dynamicScheduler.updateTask(task);
        operationLogService.log("admin", "SCHEDULE_TOGGLE",
                type + (task.isEnabled() ? " 启用" : " 停用"), "SUCCESS", null);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("task", task);
        return result;
    }

    /** 删除任务 */
    @DeleteMapping("/{type}")
    public Map<String, Object> deleteSchedule(@PathVariable String type) {
        dynamicScheduler.stopTask(type);
        scheduleTaskService.deleteByType(type);
        operationLogService.log("admin", "SCHEDULE_DELETE",
                "删除任务 " + type, "SUCCESS", null);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("message", "任务已删除");
        return result;
    }

    private Map<String, Object> errorResult(String message) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", false);
        result.put("message", message);
        return result;
    }
}
