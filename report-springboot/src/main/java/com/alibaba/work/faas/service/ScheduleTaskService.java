package com.alibaba.work.faas.service;

import com.alibaba.work.faas.model.entity.ScheduleTaskEntity;
import com.alibaba.work.faas.repository.ScheduleTaskRepository;
import com.alibaba.work.faas.schedule.ScheduleTask;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 定时任务配置服务 —— 从数据库读写。
 *
 * @author Senior Developer
 * 创建于 2026/07/08
 */
@Service
public class ScheduleTaskService {

    private final ScheduleTaskRepository repository;

    public ScheduleTaskService(ScheduleTaskRepository repository) {
        this.repository = repository;
    }

    public List<ScheduleTask> findAll() {
        return repository.findAll().stream()
                .map(this::toModel)
                .collect(Collectors.toList());
    }

    public ScheduleTask findByType(String type) {
        return repository.findByTaskType(type)
                .map(this::toModel)
                .orElse(null);
    }

    public ScheduleTask save(ScheduleTask task) {
        ScheduleTaskEntity entity = repository.findByTaskType(task.getType())
                .orElseGet(() -> {
                    ScheduleTaskEntity e = new ScheduleTaskEntity();
                    e.setTaskType(task.getType());
                    return e;
                });

        entity.setCron(task.getCron());
        entity.setTimeRangeCode(task.getTimeRangeCode());
        entity.setEnabled(task.isEnabled());
        entity.setDisplayName(task.getDisplayName());
        entity.setDescription(task.getDescription());
        entity.setUpdatedAt(LocalDateTime.now());

        repository.save(entity);
        return toModel(entity);
    }

    public void deleteByType(String type) {
        repository.findByTaskType(type).ifPresent(repository::delete);
    }

    private ScheduleTask toModel(ScheduleTaskEntity entity) {
        ScheduleTask task = new ScheduleTask();
        task.setType(entity.getTaskType());
        task.setCron(entity.getCron());
        task.setTimeRangeCode(entity.getTimeRangeCode());
        task.setEnabled(entity.isEnabled());
        task.setDisplayName(entity.getDisplayName());
        task.setDescription(entity.getDescription());
        return task;
    }
}
