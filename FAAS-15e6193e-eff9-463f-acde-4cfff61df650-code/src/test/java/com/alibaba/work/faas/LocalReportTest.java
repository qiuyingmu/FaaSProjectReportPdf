package com.alibaba.work.faas;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.work.faas.common.FaasInputs;
import com.alibaba.work.faas.service.YidaApiManager;
import com.alibaba.work.faas.report.async.ObsReportUtil;
import com.alibaba.work.faas.util.DingOpenApiUtil;
import com.alibaba.work.faas.util.YidaConnectorUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * 本地端到端测试 —— 验证全链路：FaasEntry → 查询数据 → 生成PDF → 上传OBS → 更新表单。
 *
 * <p>运行方式（在项目根目录）：
 * <pre>
 * mvn compile exec:java -Dexec.mainClass="com.alibaba.work.faas.LocalReportTest"
 * </pre>
 */
public class LocalReportTest {

    public static void main(String[] args) throws Exception {
        System.out.println("================================================");
        System.out.println("  全链路测试：平台报告 + lastMonth → OBS 存储");
        System.out.println("================================================");

        // ========================================
        //  步骤 1：获取 accessToken
        // ========================================
        String accessToken;
        try {
            accessToken = YidaApiManager.INSTANCE.getAccessToken();
            System.out.println("[Test] ✅ 获取 accessToken 成功");
        } catch (Exception e) {
            System.err.println("[Test] ❌ 获取 accessToken 失败: " + e.getMessage());
            System.err.println("[Test] 请检查 yida-secret.properties 中的凭证配置");
            return;
        }

        // ========================================
        //  步骤 2：模拟 FaaS 运行时上下文
        // ========================================
        DingOpenApiUtil.setAccessToken(accessToken);
        YidaConnectorUtil.setConsumeCode("local-test-" + System.currentTimeMillis());

        // ========================================
        //  步骤 3：构造 FaasInputs
        // ========================================
        FaasInputs faasInputs = new FaasInputs();
        Map<String, Object> yidaContext = new HashMap<>();
        yidaContext.put("accessToken", accessToken);
        yidaContext.put("consumeCode", "local-test-" + System.currentTimeMillis());
        faasInputs.setYidaContext(yidaContext);

        Map<String, Object> inputs = new HashMap<>();
        inputs.put("reportType", "project");
        inputs.put("timeRanges", "lastMonth");
        inputs.put("projectId", "");
        inputs.put("projectName", "");
        faasInputs.setInputs(inputs);

        System.out.println("[Test] 入参: " + JSON.toJSONString(faasInputs));

        // ========================================
        //  步骤 4：调用 FaasEntry（完整链路）
        // ========================================
        System.out.println();
        System.out.println(">>> 调用 FaasEntry.execute()...");
        FaasEntry entry = new FaasEntry();
        JSONObject executeResult = entry.execute(faasInputs);

        System.out.println();
        System.out.println("================================================");
        System.out.println("  阶段一 返回结果:");
        System.out.println("================================================");
        System.out.println(JSON.toJSONString(executeResult, true));

        if (!executeResult.getBooleanValue("success")) {
            System.err.println("  ❌ FaasEntry.execute() 失败: " + executeResult.getString("error"));
            return;
        }

        String formInstId = executeResult.getString("result");
        System.out.println();
        System.out.println("  ✅ 运营报告记录已创建，formInstId=" + formInstId);
        System.out.println("  后台任务正在异步执行...");

        // ========================================
        //  步骤 5：等待后台任务完成
        // ========================================
        int maxWaitSeconds = 120;
        System.out.println();
        System.out.println("  等待 " + maxWaitSeconds + " 秒给后台任务完成...");
        for (int i = 0; i < maxWaitSeconds / 5; i++) {
            Thread.sleep(5000);
            int elapsed = (i + 1) * 5;
            System.out.print("  ⏳ 已等待 " + elapsed + " 秒");
            if (elapsed >= 30 && elapsed % 15 == 0) {
                System.out.println("（请登录宜搭查看运营报告表单状态）");
            } else {
                System.out.println();
            }
        }

        // ========================================
        //  步骤 6：验证结果
        // ========================================
        System.out.println();
        System.out.println("================================================");
        System.out.println("  测试完成！");
        System.out.println("================================================");
        System.out.println();
        System.out.println("  表单实例 ID: " + formInstId);
        System.out.println("  请登录宜搭运营报告表单查看：");
        System.out.println("    ✅ 报告状态字段 → 应为「已完成」");
        System.out.println("    ✅ 备注字段 → 应包含 OBS 下载链接");
        System.out.println("    ✅ OBS 文件路径格式 → /运营报告/平台报告/{日期}_{名称}/{名称}_{时间戳}_{4位随机}.pdf");
        System.out.println();
        System.out.println("  OBS 自定义域名预览: https://obsdigitalpdf.jgjl.cn");
    }
}
