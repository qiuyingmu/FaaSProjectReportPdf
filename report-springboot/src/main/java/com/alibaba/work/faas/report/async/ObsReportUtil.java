package com.alibaba.work.faas.report.async;

import com.obs.services.ObsClient;
import com.obs.services.ObsConfiguration;
import com.obs.services.exception.ObsException;
import com.obs.services.model.ObjectMetadata;
import com.obs.services.model.PutObjectRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

/**
 * 华为 OBS 报表上传工具 —— 将生成的 PDF 上传到华为 OBS。
 *
 * <p>上传路径规范（根据 OBS 根目录）：
 * <pre>
 *   运营报告/
 *     ├── 平台报告/
 *     │   └── {yyyyMMdd}_{报告名称}/
 *     │       └── {报告名称}_{yyyyMMdd_HHmmss}_{4位随机}.pdf
 *     └── 项目报告/
 *         └── {yyyyMMdd}_{报告名称}/
 *             └── {报告名称}_{yyyyMMdd_HHmmss}_{4位随机}.pdf
 * </pre>
 *
 * <p>返回预览 URL（自定义域名）和下载 URL（OBS 原生端点）。</p>
 */
public final class ObsReportUtil {

    private static ObsClient obsClient;
    private static String bucketName;
    private static boolean initialized = false;
    private static boolean initFailed = false;

    private static final String ENDPOINT = "obs.cn-southwest-2.myhuaweicloud.com";
    private static final String CUSTOM_DOMAIN = "obsdigitalpdf.jgjl.cn";

    private static final Logger log = LoggerFactory.getLogger(ObsReportUtil.class);

    /** 从 yida-secret.properties 加载的配置（懒加载缓存） */
    private static Properties secretProps;

    /** 尝试从 classpath 加载 yida-secret.properties */
    private static synchronized Properties loadSecretProperties() {
        if (secretProps != null) return secretProps;
        secretProps = new Properties();
        try (InputStream is = ObsReportUtil.class.getClassLoader()
                .getResourceAsStream("yida-secret.properties")) {
            if (is != null) {
                secretProps.load(is);
            }
        } catch (IOException e) {
            log.debug("[ObsReportUtil] 读取 yida-secret.properties 失败: {}", e.getMessage());
        }
        return secretProps;
    }

    /**
     * 读取 OBS 配置，优先级：
     * 1. 环境变量（OBS_ACCESS_KEY）
     * 2. Java 系统属性（-DOBS_ACCESS_KEY=xxx）
     * 3. yida-secret.properties（obs.access.key）
     */
    private static String getConfig(String envKey, String propKey) {
        // 1. 环境变量
        String val = System.getenv(envKey);
        if (val != null && !val.isEmpty()) return val;
        // 2. 系统属性
        val = System.getProperty(envKey);
        if (val != null && !val.isEmpty()) return val;
        // 3. yida-secret.properties
        val = loadSecretProperties().getProperty(propKey);
        return (val != null && !val.isEmpty()) ? val : null;
    }

    /**
     * 延迟初始化 OBS 客户端，启动时不阻塞。
     * 首次调用 upload() 时检查 OBS 凭证，未配置时打印 WARN 并跳过上传。
     */
    private static synchronized void ensureInit() {
        if (initialized || initFailed) return;

        String accessKey = getConfig("OBS_ACCESS_KEY", "obs.access.key");
        String secretKey = getConfig("OBS_SECRET_KEY", "obs.secret.key");

        if (accessKey == null || secretKey == null) {
            log.warn("⚠️ OBS 未配置：缺少 OBS 凭证（环境变量 / -D参数 / yida-secret.properties），PDF 将不会上传到 OBS");
            initFailed = true;
            return;
        }

        try {
            bucketName = getConfig("OBS_BUCKET_NAME", "obs.bucket.name");
            if (bucketName == null) bucketName = "gzjgjlzx-digitization-business-pdf";
            ObsConfiguration config = new ObsConfiguration();
            config.setEndPoint(ENDPOINT);
            obsClient = new ObsClient(accessKey, secretKey, config);
            initialized = true;
            log.info("[ObsReportUtil] OBS 初始化完成: bucket={}, endpoint={}", bucketName, ENDPOINT);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    if (obsClient != null) {
                        obsClient.close();
                    }
                } catch (Exception e) {
                    log.warn("关闭 OBS 客户端失败: {}", e.getMessage());
                }
            }));
        } catch (Exception e) {
            initFailed = true;
            log.error("[ObsReportUtil] OBS 初始化失败: {}", e.getMessage());
        }
    }

    /**
     * 上传报表 PDF 到华为 OBS。
     *
     * @param pdfBytes   PDF 字节数组
     * @param reportType 报告类型（"平台报告" / "项目报告"）
     * @param reportName 报告名称（如 "平台报告_lastMonth"）
     * @return Map 包含 previewUrl 和 downloadUrl；若 OBS 未配置则返回空 Map
     */
    public static Map<String, String> upload(byte[] pdfBytes, String reportType, String reportName) {
        ensureInit();
        if (!initialized) {
            log.warn("[ObsReportUtil] OBS 未初始化，跳过上传: {}/{}", reportType, reportName);
            Map<String, String> empty = new HashMap<>();
            empty.put("previewUrl", "");
            empty.put("downloadUrl", "");
            empty.put("objectName", "");
            return empty;
        }

        // ... 原有上传逻辑不变
        String timestamp = getCurrentChinaTime();
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 4);

        // 构建对象名（目录路径）
        // 格式：运营报告/平台报告/20260707_平台报告_lastMonth/平台报告_lastMonth_20260707_1407_abcd.pdf
        String datePrefix = timestamp.substring(0, 8); // yyyyMMdd
        String folderName = datePrefix + "_" + reportName;
        String fileName = reportName + "_" + timestamp + "_" + uuid + ".pdf";
        String objectName = "运营报告/" + reportType + "/" + folderName + "/" + fileName;

        // 设置元数据
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType("application/pdf");
        metadata.setContentLength((long) pdfBytes.length);

        // 上传
        PutObjectRequest putObjectRequest = new PutObjectRequest();
        putObjectRequest.setBucketName(bucketName);
        putObjectRequest.setObjectKey(objectName);
        putObjectRequest.setInput(new ByteArrayInputStream(pdfBytes));
        putObjectRequest.setMetadata(metadata);

        try {
            obsClient.putObject(putObjectRequest);
        } catch (ObsException oe) {
            String errorMsg = formatObsError(oe);
            throw new RuntimeException("[ObsReportUtil] OBS 上传失败: " + errorMsg, oe);
        } catch (Exception e) {
            throw new RuntimeException("[ObsReportUtil] 上传文件失败: " + e.getMessage(), e);
        }

        // 构建 URL
        String previewUrl = String.format("https://%s/%s", CUSTOM_DOMAIN, objectName);
        String downloadUrl = String.format("https://%s.%s/%s", bucketName, ENDPOINT, objectName);

        log.info("上传成功: {} ({} bytes)", objectName, pdfBytes.length);
        log.debug("预览URL: {}", previewUrl); // debug level 避免日志中暴露完整 URL

        Map<String, String> urlMap = new HashMap<>();
        urlMap.put("previewUrl", previewUrl);
        urlMap.put("downloadUrl", downloadUrl);
        urlMap.put("objectName", objectName);
        return urlMap;
    }

    private static String getCurrentChinaTime() {
        ZonedDateTime chinaTime = ZonedDateTime.now(ZoneId.of("Asia/Shanghai"));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        return chinaTime.format(formatter);
    }

    private static String formatObsError(ObsException oe) {
        String errorCode = oe.getErrorCode();
        String errorMsg = oe.getErrorMessage();
        if (errorCode == null) return "OBS错误: " + errorMsg;
        switch (errorCode) {
            case "AccessDenied": return "访问被拒绝，请检查IAM权限策略";
            case "NoSuchBucket": return "存储桶不存在: " + bucketName;
            case "InvalidAccessKeyId": return "AccessKey无效，请检查配置";
            case "SignatureDoesNotMatch": return "签名不匹配，请检查SecretKey";
            case "RequestTimeTooSkewed": return "客户端时间与服务器时间偏差过大";
            default: return String.format("OBS错误: %s (错误码: %s)", errorMsg, errorCode);
        }
    }

    private ObsReportUtil() {
        throw new UnsupportedOperationException("工具类禁止实例化");
    }
}
