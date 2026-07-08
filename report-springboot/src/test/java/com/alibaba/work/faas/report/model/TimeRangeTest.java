package com.alibaba.work.faas.report.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link TimeRange} 枚举单元测试。
 *
 * @author Senior Developer
 * 创建于 2026/07/07
 */
class TimeRangeTest {

    @Test
    void fromCode_shouldParseKnownCodes() {
        assertEquals(TimeRange.LAST_WEEK, TimeRange.fromCode("lastWeek"));
        assertEquals(TimeRange.LAST_MONTH, TimeRange.fromCode("lastMonth"));
        assertEquals(TimeRange.LAST_QUARTER, TimeRange.fromCode("lastQuarter"));
        assertEquals(TimeRange.THIS_WEEK, TimeRange.fromCode("thisWeek"));
        assertEquals(TimeRange.THIS_MONTH, TimeRange.fromCode("thisMonth"));
        assertEquals(TimeRange.THIS_QUARTER, TimeRange.fromCode("thisQuarter"));
    }

    @Test
    void fromCode_shouldBeCaseInsensitive() {
        assertEquals(TimeRange.LAST_WEEK, TimeRange.fromCode("LASTWEEK"));
        assertEquals(TimeRange.LAST_MONTH, TimeRange.fromCode("LastMonth"));
    }

    @Test
    void fromCode_shouldThrowForUnknown() {
        assertThrows(IllegalArgumentException.class, () -> TimeRange.fromCode("invalid"));
        assertThrows(IllegalArgumentException.class, () -> TimeRange.fromCode(""));
    }

    @Test
    void fromLabel_shouldParseKnownLabels() {
        assertEquals(TimeRange.LAST_WEEK, TimeRange.fromLabel("上周"));
        assertEquals(TimeRange.LAST_MONTH, TimeRange.fromLabel("上月"));
        assertEquals(TimeRange.THIS_WEEK, TimeRange.fromLabel("本周"));
        assertEquals(TimeRange.THIS_MONTH, TimeRange.fromLabel("本月"));
    }

    @Test
    void parseList_shouldHandleCommaSeparated() {
        List<TimeRange> ranges = TimeRange.parseList("lastWeek, lastMonth");
        assertEquals(2, ranges.size());
        assertEquals(TimeRange.LAST_WEEK, ranges.get(0));
        assertEquals(TimeRange.LAST_MONTH, ranges.get(1));
    }

    @Test
    void parseList_shouldDefaultToThisWeek() {
        List<TimeRange> ranges = TimeRange.parseList(null);
        assertEquals(1, ranges.size());
        assertEquals(TimeRange.THIS_WEEK, ranges.get(0));

        ranges = TimeRange.parseList("");
        assertEquals(1, ranges.size());
        assertEquals(TimeRange.THIS_WEEK, ranges.get(0));
    }

    @Test
    void toReportRange_shouldReturnCorrectMappings() {
        assertEquals("week", TimeRange.THIS_WEEK.toReportRange());
        assertEquals("lastWeek", TimeRange.LAST_WEEK.toReportRange());
        assertEquals("month", TimeRange.THIS_MONTH.toReportRange());
        assertEquals("lastMonth", TimeRange.LAST_MONTH.toReportRange());
        assertEquals("quarter", TimeRange.THIS_QUARTER.toReportRange());
        assertEquals("lastQuarter", TimeRange.LAST_QUARTER.toReportRange());
    }

    @Test
    void getCode_shouldReturnCode() {
        assertEquals("lastWeek", TimeRange.LAST_WEEK.getCode());
        assertEquals("lastMonth", TimeRange.LAST_MONTH.getCode());
        assertEquals("lastQuarter", TimeRange.LAST_QUARTER.getCode());
    }

    @Test
    void getLabel_shouldReturnLabel() {
        assertEquals("上周", TimeRange.LAST_WEEK.getLabel());
        assertEquals("上月", TimeRange.LAST_MONTH.getLabel());
        assertEquals("上季度", TimeRange.LAST_QUARTER.getLabel());
    }
}
