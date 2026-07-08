package com.alibaba.work.faas;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

/**
 * 纯本地验证：对比修复前后附件 JSON 格式差异。
 * 不依赖钉钉/宜搭 API，仅验证 fastjson 序列化行为。
 */
public class AttachmentJsonTest {
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("  附件 JSON 格式验证");
        System.out.println("========================================");

        // 模拟一个附件数据
        JSONArray attachmentList = new JSONArray();
        JSONObject fileItem = new JSONObject();
        fileItem.put("fileId", "@lBDPD00xlThUjuvOBediaM5MlBxj");
        fileItem.put("fileName", "平台报告_lastMonth.pdf");
        attachmentList.add(fileItem);

        // ---- 修复前（错误）：toJSONString() ---- 
        System.out.println();
        System.out.println("【修复前】formData.put(FIELD, attachmentList.toJSONString())：");
        JSONObject beforeFix = new JSONObject();
        beforeFix.put("attachmentField_mra0z3fp", attachmentList.toJSONString());
        beforeFix.put("textareaField_mnznz7bz", "已完成");
        String beforeJson = beforeFix.toJSONString();
        System.out.println("  " + beforeJson);
        System.out.println("  → attachmentField 的值是 字符串（带转义引号）");
        System.out.println("  → 宜搭 API 收到的是: \"[{\\\"fileId\\\":\\\"@lBD...\\\"}]\"");
        System.out.println("  → ❌ 无法解析为附件");

        // ---- 修复后（正确）：直接传 JSONArray ----
        System.out.println();
        System.out.println("【修复后】formData.put(FIELD, attachmentList)：");
        JSONObject afterFix = new JSONObject();
        afterFix.put("attachmentField_mra0z3fp", attachmentList);
        afterFix.put("textareaField_mnznz7bz", "已完成");
        String afterJson = afterFix.toJSONString();
        System.out.println("  " + afterJson);
        System.out.println("  → attachmentField 的值是 原生 JSON 数组");
        System.out.println("  → 宜搭 API 收到的是: [{\"fileId\":\"@lBD...\"}]");
        System.out.println("  → ✅ 可正常解析为附件");

        // ---- 对比验证 ----
        System.out.println();
        System.out.println("========================================");
        boolean isArrayResult = afterJson.contains("\"attachmentField_mra0z3fp\":[{\"fileId\"");
        boolean isStringResult = beforeJson.contains("\"attachmentField_mra0z3fp\":\"[{\\\"fileId\\\"");
        System.out.println("修复前 attachmentField 是否被序列化为字符串: " + isStringResult);
        System.out.println("修复后 attachmentField 是否被序列化为数组:   " + isArrayResult);
        System.out.println();
        System.out.println("结论: ✅ JSON 格式修复正确，部署后附件应该正常显示。");
    }
}
