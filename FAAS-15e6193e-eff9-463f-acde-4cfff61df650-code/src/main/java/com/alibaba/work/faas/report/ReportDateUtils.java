package com.alibaba.work.faas.report;

import java.util.*;

/**
 * 报表日期计算工具类。
 *
 * <p>从 {@link ReportService} 中提取，对应 pt.js 中的 getRange()、iw()、fd() 等函数。
 * 所有方法均为静态，线程安全。</p>
 *
 * @author Senior Developer
 * 创建于 2026/07/07
 */
public final class ReportDateUtils {

    private ReportDateUtils() {}

    // ========================================
    //  内部计算
    // ========================================

    /** 获取当天 00:00:00 的毫秒数 */
    static long dayStart(Date d) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    /** 获取当天 23:59:59.999 的毫秒数 */
    static long dayEnd(Date d) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        c.set(Calendar.HOUR_OF_DAY, 23);
        c.set(Calendar.MINUTE, 59);
        c.set(Calendar.SECOND, 59);
        c.set(Calendar.MILLISECOND, 999);
        return c.getTimeInMillis();
    }

    /** 本周一 00:00:00 */
    public static Date weekStart(Date d) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        int dayOfWeek = c.get(Calendar.DAY_OF_WEEK);
        int diff = (dayOfWeek == Calendar.SUNDAY) ? -6 : Calendar.MONDAY - dayOfWeek;
        c.add(Calendar.DAY_OF_MONTH, diff);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    /** 本周日 23:59:59 */
    public static Date weekEnd(Date d) {
        Date s = weekStart(d);
        Calendar c = Calendar.getInstance();
        c.setTime(s);
        c.add(Calendar.DAY_OF_MONTH, 6);
        c.set(Calendar.HOUR_OF_DAY, 23);
        c.set(Calendar.MINUTE, 59);
        c.set(Calendar.SECOND, 59);
        c.set(Calendar.MILLISECOND, 999);
        return c.getTime();
    }

    /** 上周一 00:00:00 */
    public static Date lastWeekStart(Date d) {
        Calendar c = Calendar.getInstance();
        c.setTime(weekStart(d));
        c.add(Calendar.DAY_OF_MONTH, -7);
        return c.getTime();
    }

    /** 上周日 23:59:59 */
    public static Date lastWeekEnd(Date d) {
        Calendar c = Calendar.getInstance();
        c.setTime(weekEnd(d));
        c.add(Calendar.DAY_OF_MONTH, -7);
        return c.getTime();
    }

    /** ISO 周数计算（对应 pt.js iw()） */
    public static int isoWeek(Date d) {
        Calendar c = Calendar.getInstance();
        c.setMinimalDaysInFirstWeek(4);
        c.setFirstDayOfWeek(Calendar.MONDAY);
        c.setTime(d);
        return c.get(Calendar.WEEK_OF_YEAR);
    }

    /** 格式化日期 yyyy-MM-dd */
    public static String fd(Date d) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        return String.format("%04d-%02d-%02d",
                c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
    }


    // ========================================
    //  时间范围计算
    // ========================================

    /**
     * 时间范围值对象。
     * start/end 用于查询过滤，labelStart/labelEnd 用于显示标签。
     */
    public static class DateRange {
        public final Date start;
        public final Date end;
        public final Date labelStart;
        public final Date labelEnd;

        public DateRange(Date start, Date end, Date labelStart, Date labelEnd) {
            this.start = start;
            this.end = end;
            this.labelStart = labelStart;
            this.labelEnd = labelEnd;
        }
    }

    /**
     * 根据 range 字符串计算起止时间。
     * 对应 pt.js getRange()。
     */
    public static DateRange getRange(String range) {
        Date now = new Date();
        switch (range) {
            case "week":
                return new DateRange(weekStart(now), now, weekStart(now), weekEnd(now));
            case "lastWeek":
                return new DateRange(lastWeekStart(now), lastWeekEnd(now),
                        lastWeekStart(now), lastWeekEnd(now));
            case "month": {
                Calendar c = Calendar.getInstance();
                c.set(Calendar.DAY_OF_MONTH, 1);
                c.set(Calendar.HOUR_OF_DAY, 0);
                c.set(Calendar.MINUTE, 0);
                c.set(Calendar.SECOND, 0);
                c.set(Calendar.MILLISECOND, 0);
                Date ms = c.getTime();
                return new DateRange(ms, now, ms, now);
            }
            case "lastMonth": {
                Calendar c = Calendar.getInstance();
                c.add(Calendar.MONTH, -1);
                c.set(Calendar.DAY_OF_MONTH, 1);
                c.set(Calendar.HOUR_OF_DAY, 0);
                c.set(Calendar.MINUTE, 0);
                c.set(Calendar.SECOND, 0);
                c.set(Calendar.MILLISECOND, 0);
                Date pm = c.getTime();
                c.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH));
                c.set(Calendar.HOUR_OF_DAY, 23);
                c.set(Calendar.MINUTE, 59);
                c.set(Calendar.SECOND, 59);
                c.set(Calendar.MILLISECOND, 999);
                Date pe = c.getTime();
                return new DateRange(pm, pe, pm, pe);
            }
            case "quarter": {
                Calendar c = Calendar.getInstance();
                int month = c.get(Calendar.MONTH); // 0-based
                int qsMonth = (month / 3) * 3; // 0, 3, 6, 9
                c.set(Calendar.MONTH, qsMonth);
                c.set(Calendar.DAY_OF_MONTH, 1);
                c.set(Calendar.HOUR_OF_DAY, 0);
                c.set(Calendar.MINUTE, 0);
                c.set(Calendar.SECOND, 0);
                c.set(Calendar.MILLISECOND, 0);
                Date qs = c.getTime();
                return new DateRange(qs, now, qs, now);
            }
            case "lastQuarter": {
                Calendar c = Calendar.getInstance();
                int month = c.get(Calendar.MONTH); // 0-based
                int qsMonth = ((month / 3) - 1) * 3; // -3, 0, 3, 6
                if (qsMonth < 0) {
                    c.add(Calendar.YEAR, -1);
                    qsMonth += 12;
                }
                c.set(Calendar.MONTH, qsMonth);
                c.set(Calendar.DAY_OF_MONTH, 1);
                c.set(Calendar.HOUR_OF_DAY, 0);
                c.set(Calendar.MINUTE, 0);
                c.set(Calendar.SECOND, 0);
                c.set(Calendar.MILLISECOND, 0);
                Date qs = c.getTime();
                c.set(Calendar.MONTH, qsMonth + 2);
                c.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH));
                c.set(Calendar.HOUR_OF_DAY, 23);
                c.set(Calendar.MINUTE, 59);
                c.set(Calendar.SECOND, 59);
                c.set(Calendar.MILLISECOND, 999);
                Date qe = c.getTime();
                return new DateRange(qs, qe, qs, qe);
            }
            default:
                return getRange("week");
        }
    }

    /**
     * 生成时间范围标签（对应 pt.js rangeLabel()）。
     *
     * @param range week / lastWeek / month / lastMonth
     * @return 如 "第27周（6月29日~7月5日）" 或 "2026年7月"
     */
    public static String rangeLabel(String range) {
        Date now = new Date();
        switch (range) {
            case "week": {
                Date s = weekStart(now), e = weekEnd(now);
                return "第" + isoWeek(now) + "周（" + fd(s) + "~" + fd(e) + "）";
            }
            case "lastWeek": {
                Date s = lastWeekStart(now), e = lastWeekEnd(now);
                return "第" + (isoWeek(now) - 1) + "周（" + fd(s) + "~" + fd(e) + "）";
            }
            case "month": {
                Calendar c = Calendar.getInstance();
                return c.get(Calendar.YEAR) + "年" + (c.get(Calendar.MONTH) + 1) + "月";
            }
            case "lastMonth": {
                Calendar c = Calendar.getInstance();
                c.add(Calendar.MONTH, -1);
                return c.get(Calendar.YEAR) + "年" + (c.get(Calendar.MONTH) + 1) + "月";
            }
            case "quarter": {
                Calendar c = Calendar.getInstance();
                int month = c.get(Calendar.MONTH);
                int quarter = (month / 3) + 1;
                int qsMonth = (month / 3) * 3;
                Calendar start = Calendar.getInstance();
                start.set(Calendar.MONTH, qsMonth);
                start.set(Calendar.DAY_OF_MONTH, 1);
                Calendar end = Calendar.getInstance();
                end.set(Calendar.MONTH, qsMonth + 2);
                end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH));
                return "第" + quarter + "季度（" + fd(start.getTime()) + "~" + fd(end.getTime()) + "）";
            }
            case "lastQuarter": {
                Calendar c = Calendar.getInstance();
                int month = c.get(Calendar.MONTH);
                int quarter = ((month / 3) - 1 + 4) % 4 + 1; // 上季度编号
                int qsMonth = ((month / 3) - 1) * 3;
                Calendar start = Calendar.getInstance();
                if (qsMonth < 0) { start.add(Calendar.YEAR, -1); qsMonth += 12; }
                start.set(Calendar.MONTH, qsMonth);
                start.set(Calendar.DAY_OF_MONTH, 1);
                Calendar end = Calendar.getInstance();
                if ((month / 3) - 1 < 0) { end.add(Calendar.YEAR, -1); }
                end.set(Calendar.MONTH, qsMonth + 2);
                end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH));
                return "第" + quarter + "季度（" + fd(start.getTime()) + "~" + fd(end.getTime()) + "）";
            }
            default:
                return rangeLabel("week");
        }
    }

    /**
     * 时间段名称（对应 pt.js periodName()）。
     *
     * @param range week / lastWeek / month / lastMonth
     * @return 本周 / 上周 / 本月 / 上月
     */
    public static String periodName(String range) {
        switch (range) {
            case "week": return "本周";
            case "lastWeek": return "上周";
            case "month": return "本月";
            case "lastMonth": return "上月";
            case "quarter": return "本季度";
            case "lastQuarter": return "上季度";
            default: return "未知";
        }
    }
}
