package com.alibaba.work.faas.report.async;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.work.faas.service.YidaApiManager;
import com.aliyun.dingtalkyida_2_0.models.SaveFormDataRequest;
import com.aliyun.dingtalkyida_2_0.models.UpdateFormDataRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 宜搭表单操作封装 —— 运营报告表单的创建、更新、OBS 文件上传。
 *
 * @author Senior Developer
 * 创建于 2026/07/07
 */
@Service
public class YidaFormUpdater {

    private static final Logger log = LoggerFactory.getLogger(YidaFormUpdater.class);

    private final YidaApiManager api;

    public YidaFormUpdater(YidaApiManager api) {
        this.api = api;
    }


    // ========================================
    //  运营报告表单编码
    // ========================================

    /** 运营报告表单 UUID */
    public static final String FORM_REPORT = "FORM-DBE928172E9E473B84E5ABB0E244731489UR";

    public static final String FIELD_NAME = "textField_mnznz7bg";               // 运营报告名称
    public static final String FIELD_TYPE = "radioField_mr8y19k0";               // 运营报告类型
    public static final String FIELD_DATE_RANGE = "textField_mra0z3ft";          // 报告日期范围
    public static final String FIELD_ATTACHMENT = "attachmentField_mra0z3fp";    // 运营报告附件
    public static final String FIELD_REMARK = "textareaField_mnznz7bz";          // 备注
    public static final String FIELD_UUID = "textField_mnzhv2ur";                // 唯一编码
    public static final String FIELD_REPORT_STATUS = "radioField_mra8tlnq";      // 报告状态

    // 系统信息字段
    public static final String FIELD_SUBMITTER = "employeeField_mnzhv2ug";      // 提交人
    public static final String FIELD_SUBMIT_DATE = "dateField_mnzhv2ul";         // 提交日期
    public static final String FIELD_SORT = "numberField_mo0tb9rw";             // 显示排序
    public static final String FIELD_STATUS = "radioField_mo0tk6eg";             // 状态（启用/禁用）

    // 提交人固定值（钉钉用户 ID）
    public static final String DEFAULT_SUBMITTER = "02042920465523597121";


    // ========================================
    //  创建运营报告记录（阶段一）
    // ========================================

    /**
     * 创建运营报告表单记录，状态标记为"生成中"。
     *
     * <p>对于周期报告（周报/月报/季报）：</p>
     * <ul>
     *   <li>reportName — 运营报告名称（不含扩展名，如 "运营报告-周报-6月第1周-(2026-06-01 ~ 2026-06-07)"）</li>
     *   <li>reportType — 周期类型标签（"周报" / "月报" / "季报"，存入 radioField_mr8y19k0）</li>
     *   <li>dateRange — 日期范围标签（如 "6月第1周"）</li>
     *   <li>timeRanges — 时间范围描述（如 "2026-06-01 ~ 2026-06-07"）</li>
     * </ul>
     *
     * @param reportName  报告名称
     * @param reportType  报告周期类型（周报/月报/季报）
     * @param dateRange   日期范围标签
     * @param timeRanges  时间范围描述
     * @return 新建的表单实例 ID
     * @throws Exception 创建失败时抛出
     */
    public String createReportRecord(String reportName, String reportType,
                                      String dateRange, String timeRanges) throws Exception {
        // 构建表单数据
        JSONObject formData = new JSONObject();
        formData.put(FIELD_NAME, reportName);
        formData.put(FIELD_TYPE, reportType);
        formData.put(FIELD_DATE_RANGE, dateRange);
        formData.put(FIELD_REPORT_STATUS, "生成中");
        formData.put(FIELD_REMARK, "生成中 | 时间范围: " + timeRanges);
        formData.put(FIELD_UUID, UUID.randomUUID().toString().replace("-", "").toUpperCase());

        // 系统信息字段
        JSONArray submitters = new JSONArray();
        submitters.add(DEFAULT_SUBMITTER);
        formData.put(FIELD_SUBMITTER, submitters);
        formData.put(FIELD_SUBMIT_DATE, System.currentTimeMillis());
        formData.put(FIELD_SORT, 50);
        formData.put(FIELD_STATUS, "启用");

        SaveFormDataRequest request = new SaveFormDataRequest()
                .setAppType(api.getAppType())
                .setSystemToken(api.getSystemToken())
                .setUserId(api.getDefaultUserId())
                .setFormUuid(FORM_REPORT)
                .setFormDataJson(formData.toJSONString());

        String instanceId = api.saveFormData(request);
        log.info("[YidaFormUpdater] 创建运营报告记录成功: " + instanceId);

        return instanceId;
    }


    // ========================================
    //  上传 PDF 到华为 OBS
    // ========================================

    /**
     * 将 PDF 上传到华为 OBS，返回 OBS 对象信息。
     * <p>已替换原有的钉钉 media/upload 上传方式。</p>
     *
     * @param pdfBytes   PDF 文件字节数组
     * @param reportType 报告类型（"平台报告" / "项目报告"）
     * @param reportName 报告名称（如 "平台报告_lastMonth"）
     * @return Map 包含 previewUrl、downloadUrl、objectName
     */
    public Map<String, String> uploadToObs(byte[] pdfBytes, String reportType, String reportName) {
        return ObsReportUtil.upload(pdfBytes, reportType, reportName);
    }


    // ========================================
    //  更新运营报告记录（阶段二）
    // ========================================

    /**
     * 更新运营报告记录：添加 OBS 附件 + 更新状态 + 日期范围。
     * <p>附件字段格式：宜搭附件对象数组，包含 downloadUrl/name/previewUrl/url/ext。</p>
     *
     * @param formInstId  表单实例 ID
     * @param obsFiles    OBS 文件信息列表（每个 Map 含 previewUrl、objectName）
     * @param dateRange   报告日期范围描述
     * @param success     是否成功
     * @param message     状态消息
     * @throws Exception 更新失败时抛出
     */
    public void updateReportRecord(String formInstId, List<Map<String, String>> obsFiles,
                                    String dateRange, boolean success,
                                    String message) throws Exception {
        // 构建附件字段（宜搭附件对象数组格式）
        JSONArray attachmentList = new JSONArray();
        if (obsFiles != null) {
            for (Map<String, String> obs : obsFiles) {
                String url = obs.get("previewUrl");
                String objectName = obs.get("objectName");
                String fileName = (objectName != null)
                        ? objectName.substring(objectName.lastIndexOf('/') + 1)
                        : "report.pdf";

                JSONObject fileObj = new JSONObject();
                fileObj.put("downloadUrl", url);
                fileObj.put("name", fileName);
                fileObj.put("previewUrl", url);
                fileObj.put("url", url);
                fileObj.put("ext", "pdf");
                attachmentList.add(fileObj);
            }
        }

        // 构建备注
        StringBuilder remark = new StringBuilder();
        if (success) {
            if (message != null && !message.isEmpty() && !"已完成".equals(message)) {
                // 自定义成功消息（如 "该时间范围内无数据"）
                remark.append(message);
            } else {
                remark.append("已完成");
            }
            if (obsFiles != null && !obsFiles.isEmpty()) {
                remark.append("\nPDF下载链接：\n");
                for (int i = 0; i < obsFiles.size(); i++) {
                    remark.append("  ").append(i + 1).append(". ")
                          .append(obsFiles.get(i).get("previewUrl")).append("\n");
                }
            }
        } else {
            remark.append("失败: ").append(message);
        }

        String status = success ? "已完成" : "失败";

        JSONObject formData = new JSONObject();
        if (!attachmentList.isEmpty()) {
            formData.put(FIELD_ATTACHMENT, attachmentList);
        }
        formData.put(FIELD_DATE_RANGE, dateRange != null ? dateRange : "");
        formData.put(FIELD_REPORT_STATUS, status);
        formData.put(FIELD_REMARK, remark.toString());

        UpdateFormDataRequest request = new UpdateFormDataRequest()
                .setAppType(api.getAppType())
                .setSystemToken(api.getSystemToken())
                .setUserId(api.getDefaultUserId())
                .setFormUuid(FORM_REPORT)
                .setFormInstanceId(formInstId)
                .setUpdateFormDataJson(formData.toJSONString())
                .setUseLatestVersion(true);

        api.updateFormData(request);
        log.info("[YidaFormUpdater] 更新运营报告记录成功: " + formInstId
                + " -> " + status + "，附件 " + attachmentList.size() + " 个");
    }
}
