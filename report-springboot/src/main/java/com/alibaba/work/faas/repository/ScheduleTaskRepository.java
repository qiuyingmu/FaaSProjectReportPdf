package com.alibaba.work.faas.repository;

import com.alibaba.work.faas.model.entity.ScheduleTaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 定时任务配置数据访问层。
 *
 * @author Senior Developer
 * 创建于 2026/07/08
 */
@Repository
public interface ScheduleTaskRepository extends JpaRepository<ScheduleTaskEntity, Long> {
    Optional<ScheduleTaskEntity> findByTaskType(String taskType);
}
