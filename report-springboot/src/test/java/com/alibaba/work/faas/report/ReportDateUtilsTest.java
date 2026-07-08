package com.alibaba.work.faas.report;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link ReportDateUtils} 单元测试。
 *
 * @author Senior Developer
 * 创建于 2026/07/07
 */
class ReportDateUtilsTest {

    @Test
    void rangeLabel_shouldReturnCorrectFormat() {
        String label = ReportDateUtils.rangeLabel("lastMonth");
        assertNotNull(label);
        assertTrue(label.contains("年") && label.contains("月"));
    }

    @Test
    void periodName_shouldReturnCorrectNames() {
        assertEquals("上周", ReportDateUtils.periodName("lastWeek"));
        assertEquals("上月", ReportDateUtils.periodName("lastMonth"));
        assertEquals("上季度", ReportDateUtils.periodName("lastQuarter"));
        assertEquals("本周", ReportDateUtils.periodName("week"));
        assertEquals("本月", ReportDateUtils.periodName("month"));
        assertEquals("本季度", ReportDateUtils.periodName("quarter"));
    }

    @Test
    void fd_shouldFormatDate() {
        java.util.Date date = new java.util.Date();
        String formatted = ReportDateUtils.fd(date);
        assertNotNull(formatted);
        assertTrue(formatted.matches("\\d{4}-\\d{2}-\\d{2}"));
    }

    @Test
    void getRange_shouldReturnValidRange() {
        ReportDateUtils.DateRange range = ReportDateUtils.getRange("lastMonth");
        assertNotNull(range);
        assertNotNull(range.start);
        assertNotNull(range.end);
        assertTrue(range.start.getTime() <= range.end.getTime());
    }
}
