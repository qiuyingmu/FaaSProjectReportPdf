package com.alibaba.work.faas.service;

import com.alibaba.work.faas.entity.ApiKey;
import com.alibaba.work.faas.repository.ApiKeyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * API Key 管理服务。
 *
 * <p>Key 格式：<code>yida-</code> + 48 个随机字符（共 53 字符）。BCrypt 哈希后存储，明文不保存。</p>
 */
@Service
public class ApiKeyService {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyService.class);

    /** Key 前缀：便于日志筛选、辨识 */
    private static final String KEY_PREFIX = "yida-";

    private static final int RANDOM_BYTES = 24; // 24 bytes = 48 hex chars
    private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();

    private final ApiKeyRepository apiKeyRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final SecureRandom random = new SecureRandom();

    /** 验证用的回退 Key（.env 中配置的 yida.connector.api.key），向后兼容旧部署 */
    @Autowired(required = false)
    @org.springframework.beans.factory.annotation.Value("${yida.connector.api.key:}")
    private String fallbackApiKey;

    public ApiKeyService(ApiKeyRepository apiKeyRepository) {
        this.apiKeyRepository = apiKeyRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    /**
     * 验证 API Key：先尝试数据库（启用状态），再回退到 .env 配置的 Key（兼容老部署）。
     *
     * @param apiKey 客户端传来的明文 Key
     * @return 是否有效
     */
    public boolean verifyApiKey(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return false;
        }

        // 1. 数据库扫描（启用状态）
        for (ApiKey stored : apiKeyRepository.findAllByEnabledTrue()) {
            if (passwordEncoder.matches(apiKey, stored.getKeyHash())) {
                // 异步更新最后使用时间（不阻塞请求）
                updateLastUsedAsync(stored.getId());
                return true;
            }
        }

        // 2. .env 回退（兼容老部署）
        if (fallbackApiKey != null && !fallbackApiKey.isBlank() && fallbackApiKey.equals(apiKey)) {
            return true;
        }

        return false;
    }

    private void updateLastUsedAsync(Long id) {
        try {
            apiKeyRepository.findById(id).ifPresent(k -> {
                k.setLastUsedAt(Instant.now());
                apiKeyRepository.save(k);
            });
        } catch (Exception e) {
            log.warn("[ApiKeyService] 更新 lastUsedAt 失败: {}", e.getMessage());
        }
    }

    /**
     * 创建新 API Key。
     *
     * @param name 用户指定的 Key 名称
     * @return 创建结果（包含完整 Key，仅此一次返回）
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
        entity = apiKeyRepository.save(entity);

        log.info("[ApiKeyService] 创建新 API Key: id={}, name={}, prefix={}", entity.getId(), name, prefix);
        return new CreateResult(entity, fullKey);
    }

    /** 列出所有 Key（不含明文） */
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
        log.info("[ApiKeyService] 删除 API Key: id={}", id);
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