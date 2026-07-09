package com.alibaba.work.faas.repository;

import com.alibaba.work.faas.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 用户数据访问层。
 *
 * @author Senior Developer
 * 创建于 2026/07/08
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);

    /** 统计除指定 ID 外还有多少启用中的管理员 */
    @Query("SELECT COUNT(u) FROM User u WHERE u.enabled = true AND u.id <> ?1")
    long countOtherActiveAdmins(Long excludeId);
}
