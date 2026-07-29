package com.alibaba.work.faas.service;

import com.alibaba.work.faas.entity.ApiKey;
import com.alibaba.work.faas.entity.ApiKeyUsageLog;
import com.alibaba.work.faas.repository.ApiKeyRepository;
import com.alibaba.work.faas.repository.ApiKeyUsageLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * API Key 管理服务。
 *
 * <p>Key 格式：<code>yida-</code> + 48 个随机字符（共 53 字符）。BCrypt 哈希后存储，明文不保存。</p>
 */
@Service
public class ApiKeyService {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyService.class);

    /** Key 前缀 */
    private static final String KEY_PREFIX = "yida-";

    private static final int RANDOM_BYTES = 24; // 24 bytes = 48 hex chars
    private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();

    private final ApiKeyRepository apiKeyRepository;
    private final ApiKeyUsageLogRepository usageLogRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final SecureRandom random = new SecureRandom();

    public ApiKeyService(ApiKeyRepository apiKeyRepository,
                          ApiKeyUsageLogRepository usageLogRepository) {
        this.apiKeyRepository = apiKeyRepository;
        this.usageLogRepository = usageLogRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    /**
     * 验证 API Key：扫描数据库所有启用状态的 Key。
     * 命中后自动累加当日调用次数 +1，更新总次数 +1，更新最后使用时间。
     */
    @Transactional
    public boolean verifyApiKey(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return false;
        }

        for (ApiKey stored : apiKeyRepository.findAllByEnabledTrue()) {
            if (passwordEncoder.matches(apiKey, stored.getKeyHash())) {
                recordUsage(stored.getId());
                return true;
            }
        }
        return false;
    }

    /** 记录一次调用：当日 +1、总次数 +1、最后使用时间 now */
    private void recordUsage(Long keyId) {
        try {
            // 单条原子 SQL：当天 +1
            usageLogRepository.incrementDaily(keyId, LocalDate.now());

            // 更新总次数 +1 和最后使用时间
            apiKeyRepository.findById(keyId).ifPresent(k -> {
                k.setTotalRequests(k.getTotalRequests() + 1);
                k.setLastUsedAt(Instant.now());
                apiKeyRepository.save(k);
            });
        } catch (Exception e) {
            log.warn("[ApiKeyService] 记录调用统计失败: {}", e.getMessage());
        }
    }

    /**
     * 创建新 API Key。
     */
    @Transactional
    public CreateResult create(String name) {
        String fullKey = generateRandomKey();
        String prefix = fullKey.substring(0, Math.min(8, fullKey.length()))
                + "..." + fullKey.substring(fullKey.length() - 4);

        ApiKey entity = new ApiKey();
        entity.setName(name);
        entity.setKeyPrefix(prefix);
        entity.setKeyHash(passwordEncoder.encode(fullKey));
        entity.setCreatedAt(Instant.now());
        entity.setEnabled(true);
        entity.setTotalRequests(0);
        entity = apiKeyRepository.save(entity);

        log.info("[ApiKeyService] 创建新 API Key: id={}, name={}, prefix={}", entity.getId(), name, prefix);
        return new CreateResult(entity, fullKey);
    }

    /** 列出所有 Key（不含明文哈希） */
    public List<ApiKey> list() {
        return apiKeyRepository.findAllByOrderByCreatedAtDesc();
    }

    /** 启用 / 禁用 */
    @Transactional
    public ApiKey setEnabled(Long id, boolean enabled) {
        ApiKey k = apiKeyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Key 不存在: " + id));
        k.setEnabled(enabled);
        return apiKeyRepository.save(k);
    }

    /** 删除 Key */
    @Transactional
    public void delete(Long id) {
        apiKeyRepository.deleteById(id);
        // 同步删除统计记录
        usageLogRepository.findRange(id, LocalDate.of(1970, 1, 1), LocalDate.of(2999, 12, 31))
                .forEach(log -> usageLogRepository.deleteById(log.getId()));
        log.info("[ApiKeyService] 删除 API Key: id={}", id);
    }

    /** 查询指定 Key 在指定日期范围内的每日统计 */
    public List<ApiKeyUsageLog> getStats(Long keyId, LocalDate from, LocalDate to) {
        return usageLogRepository.findRange(keyId, from, to);
    }

    /** 生成随机 Key：yida- + 48 hex 字符 */
    private String generateRandomKey() {
        byte[] bytes = new byte[RANDOM_BYTES];
        random.nextBytes(bytes);
        char[] hex = new char[RANDOM_BYTES * 2];
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            hex[i * 2] = HEX_CHARS[v >>> 4];
            hex[i * 2 + 1] = HEX_CHARS[v & 0x0F];
        }
        return KEY_PREFIX + new String(hex);
    }

    /** 创建结果：实体 + 明文 Key（仅创建时返回一次） */
    public static class CreateResult {
        public final ApiKey apiKey;
        public final String fullKey;

        public CreateResult(ApiKey apiKey, String fullKey) {
            this.apiKey = apiKey;
            this.fullKey = fullKey;
        }
    }
}