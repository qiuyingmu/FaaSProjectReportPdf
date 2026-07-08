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
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
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

    private static final ObsClient obsClient;
    private static final String BUCKET_NAME;
    private static final String ENDPOINT;
    private static final String CUSTOM_DOMAIN = "obsdigitalpdf.jgjl.cn";

    private static final Logger log = LoggerFactory.getLogger(ObsReportUtil.class);

    static {
        ENDPOINT = "obs.cn-southwest-2.myhuaweicloud.com";
        String accessKey = "HPUAE0EESY1ENPLZNPGF";
        String secretKey = "ljVV3WvemaymdQneIGI3ztS6CxJjhv7AWbDWAuCg";
        BUCKET_NAME = "gzjgjlzx-digitization-business-pdf";

        ObsConfiguration config = new ObsConfiguration();
        config.setEndPoint(ENDPOINT);

        obsClient = new ObsClient(accessKey, secretKey, config);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                if (obsClient != null) {
                    obsClient.close();
                }
            } catch (Exception e) {
                log.warn("关闭 OBS 客户端失败: {}", e.getMessage());
            }
        }));
    }

    /**
     * 上传报表 PDF 到华为 OBS。
     *
     * @param pdfBytes   PDF 字节数组
     * @param reportType 报告类型（"平台报告" / "项目报告"）
     * @param reportName 报告名称（如 "平台报告_lastMonth"）
     * @return Map 包含 previewUrl 和 downloadUrl
     * @throws RuntimeException 上传失败时抛出
     */
    public static Map<String, String> upload(byte[] pdfBytes, String reportType, String reportName) {
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
        putObjectRequest.setBucketName(BUCKET_NAME);
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
        String downloadUrl = String.format("https://%s.%s/%s", BUCKET_NAME, ENDPOINT, objectName);

        log.info("上传成功: {} ({} bytes)", objectName, pdfBytes.length);
        log.info("预览URL: {}", previewUrl);

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
            case "NoSuchBucket": return "存储桶不存在: " + BUCKET_NAME;
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
