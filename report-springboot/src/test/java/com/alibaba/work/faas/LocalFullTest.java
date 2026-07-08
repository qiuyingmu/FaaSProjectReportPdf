package com.alibaba.work.faas;

import com.alibaba.work.faas.report.ReportDateUtils;
import com.alibaba.work.faas.report.ReportService;
import com.alibaba.work.faas.report.async.YidaFormUpdater;
import com.alibaba.work.faas.report.model.ReportRequest;
import com.alibaba.work.faas.report.model.ReportResult;
import com.alibaba.work.faas.report.model.ReportType;
import com.alibaba.work.faas.report.model.TimeRange;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 全链路集成测试 —— 验证报告生成流程。
 *
 * <p>注意：此测试会真实调用钉钉 API 和 OBS，需要网络连接和有效凭证。</p>
 *
 * @author Senior Developer
 * 创建于 2026/07/07
 */
@SpringBootTest
public class LocalFullTest {

    @Autowired
    private ReportService reportService;

    @Autowired
    private YidaFormUpdater formUpdater;

    /**
     * 测试：生成平台报告 + 项目报告（上月）。
     * 验证 PDF 能否正常生成以及记录数大于 0。
     */
    @Test
    @Disabled("手动测试：需要钉钉和 OBS 凭证")
    void testGeneratePlatformAndProjectReport() throws Exception {
        // 平台报告 - 上月
        ReportRequest platReq = new ReportRequest(
                ReportType.PLATFORM,
                Collections.singletonList(TimeRange.LAST_MONTH),
                null, null);
        List<ReportResult> platResults = reportService.generate(platReq);
        assertNotNull(platResults);
        assertFalse(platResults.isEmpty(), "平台报告应有数据");
        assertTrue(platResults.get(0).getPdfSize() > 0, "PDF 应有内容");
        System.out.println("✅ 平台报告: " + platResults.get(0).getTotalRecords() + " 条");

        // 项目报告 - 上月（全项目汇总）
        ReportRequest projReq = new ReportRequest(
                ReportType.PROJECT,
                Collections.singletonList(TimeRange.LAST_MONTH),
                null, null);
        List<ReportResult> projResults = reportService.generate(projReq);
        assertNotNull(projResults);
        assertFalse(projResults.isEmpty(), "项目报告应有数据");
        assertTrue(projResults.get(0).getPdfSize() > 0, "PDF 应有内容");
        System.out.println("✅ 项目报告: " + projResults.get(0).getTotalRecords() + " 条");
    }
}
