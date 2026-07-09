package com.alibaba.work.faas.controller;

import com.alibaba.work.faas.common.ApiResponse;
import com.alibaba.work.faas.common.UserDTO;
import com.alibaba.work.faas.model.entity.User;
import com.alibaba.work.faas.repository.UserRepository;
import com.alibaba.work.faas.service.OperationLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 管理员管理 API。
 *
 * <h3>安全规则</h3>
 * <ul>
 *   <li>不能封禁自己</li>
 *   <li>不能删除最后一个启用中的管理员</li>
 *   <li>所有操作记录审计日志</li>
 *   <li>密码最小长度 6 位</li>
 * </ul>
 *
 * @author Senior Developer
 * 创建于 2026/07/08
 */
@RestController
@RequestMapping("/api/admin/users")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final UserRepository userRepository;
    private final OperationLogService operationLogService;
    private final PasswordEncoder passwordEncoder;

    public UserController(UserRepository userRepository,
                          OperationLogService operationLogService) {
        this.userRepository = userRepository;
        this.operationLogService = operationLogService;
        this.passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    // ========================================
    //  查询所有管理员
    // ========================================

    @GetMapping
    public ApiResponse<List<UserDTO>> list() {
        List<UserDTO> users = userRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        return ApiResponse.success(users).withMessage("共 " + users.size() + " 个管理员");
    }

    // ========================================
    //  新增管理员
    // ========================================

    @PostMapping(consumes = "application/json")
    public ApiResponse<UserDTO> create(@RequestBody UserDTO dto, Authentication auth) {
        // 参数校验
        String username = dto.getUsername();
        String password = dto.getPassword();
        if (username == null || username.trim().isEmpty()) {
            return ApiResponse.badRequest("用户名不能为空");
        }
        if (password == null || password.length() < 6) {
            return ApiResponse.badRequest("密码至少 6 位");
        }
        if (userRepository.existsByUsername(username.trim())) {
            return ApiResponse.badRequest("用户名已存在: " + username);
        }

        User user = new User(username.trim(), passwordEncoder.encode(password));
        user.setRole(dto.getRole() != null ? dto.getRole() : "ADMIN");
        userRepository.save(user);

        operationLogService.log(auth.getName(), "USER_CREATE",
                "创建管理员: " + username, "SUCCESS", null);
        log.info("✅ 管理员 {} 创建了用户: {}", auth.getName(), username);

        return ApiResponse.success(toDTO(user)).withMessage("管理员已创建");
    }

    // ========================================
    //  修改管理员（角色、启用状态）
    // ========================================

    @PutMapping(value = "/{id}", consumes = "application/json")
    public ApiResponse<UserDTO> update(@PathVariable Long id,
                                        @RequestBody UserDTO dto,
                                        Authentication auth) {
        User user = userRepository.findById(id)
                .orElse(null);
        if (user == null) {
            return ApiResponse.badRequest("用户不存在");
        }

        // 安全：不能封禁自己
        if (!dto.isEnabled() && user.getUsername().equals(auth.getName())) {
            return ApiResponse.badRequest("不能封禁自己的账号");
        }

        // 安全：不能把最后一个管理员禁用
        if (!dto.isEnabled() && isLastActiveAdmin(id)) {
            return ApiResponse.badRequest("不能禁用最后一个管理员");
        }

        user.setEnabled(dto.isEnabled());
        if (dto.getRole() != null) {
            user.setRole(dto.getRole());
        }
        userRepository.save(user);

        operationLogService.log(auth.getName(), "USER_UPDATE",
                (dto.isEnabled() ? "启用" : "封禁") + "管理员: " + user.getUsername(),
                "SUCCESS", null);
        log.info("✅ 管理员 {} 更新了用户 {}: enabled={}", auth.getName(), user.getUsername(), dto.isEnabled());

        return ApiResponse.success(toDTO(user)).withMessage("已更新");
    }

    // ========================================
    //  重置密码
    // ========================================

    @PutMapping(value = "/{id}/password", consumes = "application/json")
    public ApiResponse<Object> resetPassword(@PathVariable Long id,
                                            @RequestBody UserDTO dto,
                                            Authentication auth) {
        String newPassword = dto.getPassword();
        if (newPassword == null || newPassword.length() < 6) {
            return ApiResponse.badRequest("密码至少 6 位");
        }

        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            return ApiResponse.badRequest("用户不存在");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        operationLogService.log(auth.getName(), "USER_PASSWORD",
                "重置密码: " + user.getUsername(), "SUCCESS", null);
        log.info("✅ 管理员 {} 重置了 {} 的密码", auth.getName(), user.getUsername());

        return ApiResponse.success().withMessage("密码已重置");
    }

    // ========================================
    //  删除管理员
    // ========================================

    @DeleteMapping("/{id}")
    public ApiResponse<Object> delete(@PathVariable Long id, Authentication auth) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            return ApiResponse.badRequest("用户不存在");
        }

        // 安全：不能删除自己
        if (user.getUsername().equals(auth.getName())) {
            return ApiResponse.badRequest("不能删除自己的账号");
        }

        // 安全：不能删除最后一个管理员
        if (isLastActiveAdmin(id)) {
            return ApiResponse.badRequest("不能删除最后一个管理员");
        }

        userRepository.delete(user);

        operationLogService.log(auth.getName(), "USER_DELETE",
                "删除管理员: " + user.getUsername(), "SUCCESS", null);
        log.info("✅ 管理员 {} 删除了用户: {}", auth.getName(), user.getUsername());

        return ApiResponse.success().withMessage("已删除");
    }

    // ========================================
    //  工具方法
    // ========================================

    /** 判断是否只剩一个启用的管理员（排除指定 id） */
    private boolean isLastActiveAdmin(Long excludeId) {
        return userRepository.countOtherActiveAdmins(excludeId) == 0;
    }

    /** Entity → DTO，不暴露密码哈希 */
    private UserDTO toDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setRole(user.getRole());
        dto.setEnabled(user.isEnabled());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setLastLogin(user.getLastLogin());
        return dto;
    }
}
