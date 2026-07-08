package com.alibaba.work.faas.report.model;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link ReportRequest} 单元测试。
 *
 * @author Senior Developer
 * 创建于 2026/07/07
 */
class ReportRequestTest {

    @Test
    void constructor_shouldCreateValidRequest() {
        ReportRequest req = new ReportRequest(
                ReportType.PLATFORM,
                Collections.singletonList(TimeRange.LAST_MONTH),
                null, null);
        assertEquals(ReportType.PLATFORM, req.getType());
        assertEquals(1, req.getTimeRanges().size());
        assertEquals(TimeRange.LAST_MONTH, req.getTimeRanges().get(0));
        assertTrue(req.isPlatformReport());
        assertFalse(req.isProjectReport());
    }

    @Test
    void constructor_shouldThrowWhenTypeNull() {
        assertThrows(IllegalArgumentException.class, () ->
                new ReportRequest(null, Collections.singletonList(TimeRange.LAST_WEEK), null, null));
    }

    @Test
    void constructor_shouldThrowWhenTimeRangesEmpty() {
        assertThrows(IllegalArgumentException.class, () ->
                new ReportRequest(ReportType.PLATFORM, Collections.emptyList(), null, null));
    }

    @Test
    void fromInput_shouldParsePlatformRequest() {
        Map<String, Object> input = new HashMap<>();
        input.put("reportType", "platform");
        input.put("timeRanges", "lastMonth");

        ReportRequest req = ReportRequest.fromInput(input);
        assertEquals(ReportType.PLATFORM, req.getType());
        assertEquals(1, req.getTimeRanges().size());
        assertEquals(TimeRange.LAST_MONTH, req.getTimeRanges().get(0));
    }

    @Test
    void fromInput_shouldParseProjectRequest() {
        Map<String, Object> input = new HashMap<>();
        input.put("reportType", "project");
        input.put("timeRanges", "lastWeek");
        input.put("projectId", "INST-12345");

        ReportRequest req = ReportRequest.fromInput(input);
        assertEquals(ReportType.PROJECT, req.getType());
        assertEquals(TimeRange.LAST_WEEK, req.getTimeRanges().get(0));
        assertEquals("INST-12345", req.getProjectId());
    }

    @Test
    void fromInput_shouldHandleMultipleTimeRanges() {
        Map<String, Object> input = new HashMap<>();
        input.put("reportType", "platform");
        input.put("timeRanges", "lastWeek, lastMonth, lastQuarter");

        ReportRequest req = ReportRequest.fromInput(input);
        assertEquals(3, req.getTimeRanges().size());
        assertEquals(TimeRange.LAST_WEEK, req.getTimeRanges().get(0));
        assertEquals(TimeRange.LAST_MONTH, req.getTimeRanges().get(1));
        assertEquals(TimeRange.LAST_QUARTER, req.getTimeRanges().get(2));
    }

    @Test
    void fromInput_shouldThrowWhenEmpty() {
        assertThrows(IllegalArgumentException.class, () ->
                ReportRequest.fromInput(Collections.emptyMap()));
        assertThrows(IllegalArgumentException.class, () ->
                ReportRequest.fromInput(null));
    }

    @Test
    void toString_shouldContainTypeAndRanges() {
        ReportRequest req = new ReportRequest(
                ReportType.PROJECT,
                Collections.singletonList(TimeRange.LAST_WEEK),
                "INST-123", "测试项目");
        String str = req.toString();
        assertTrue(str.contains("项目报告"));
        assertTrue(str.contains("LAST_WEEK"));
        assertTrue(str.contains("INST-123"));
    }
}
