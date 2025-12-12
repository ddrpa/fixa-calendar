package cc.ddrpa.fixa;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ReverseWorkdayTests {

    private static final FixaCalendar calendar = new FixaCalendarBuilder().startWeekendCalcAfter(LocalDate.of(2025, 1, 1)).setWeekendType(FixaWeekendEnum.SATURDAY_AND_SUNDAY).build();

    /**
     * 测试结束日期是工作日的情况
     */
    @Test
    void endDateIsWorkdayTest() {
        // 区间中没有非工作日
        LocalDate supposedStartDate = calendar.reverseWorkday(LocalDate.of(2025, 7, 4), Duration.ofDays(3), false);
        assertEquals(LocalDate.of(2025, 7, 1), supposedStartDate);

        // 区间跨过非工作日
        supposedStartDate = calendar.reverseWorkday(LocalDate.of(2025, 7, 7), Duration.ofDays(4), false);
        assertEquals(LocalDate.of(2025, 7, 1), supposedStartDate);

        // 区间计数落在非工作日上时向前推进
        supposedStartDate = calendar.reverseWorkday(LocalDate.of(2025, 7, 7), Duration.ofDays(1), false);
        assertEquals(LocalDate.of(2025, 7, 4), supposedStartDate);
    }

    // 测试结束日期是非工作日的情况，不考虑结束日期是否为工作日
    @Test
    void endDateAllowedToBeOffday() {
        // 从非工作日开始，向前推进
        LocalDate supposedStartDate = calendar.reverseWorkday(LocalDate.of(2025, 7, 5), Duration.ofDays(1), false);
        assertEquals(LocalDate.of(2025, 7, 4), supposedStartDate);

        supposedStartDate = calendar.reverseWorkday(LocalDate.of(2025, 7, 6), Duration.ofDays(1), false);
        assertEquals(LocalDate.of(2025, 7, 4), supposedStartDate);

        // 区间计数落在非工作日上时向前推进
        supposedStartDate = calendar.reverseWorkday(LocalDate.of(2025, 7, 13), Duration.ofDays(4), false);
        assertEquals(LocalDate.of(2025, 7, 8), supposedStartDate);

        supposedStartDate = calendar.reverseWorkday(LocalDate.of(2025, 7, 13), Duration.ofDays(6), false);
        assertEquals(LocalDate.of(2025, 7, 4), supposedStartDate);
    }

    // 测试结束日期是非工作日的情况，不考虑结束日期是否为工作日
    @Test
    void endDateMustBeWorkday() {
        // 从非工作日开始，向前推进
        LocalDate supposedStartDate = calendar.reverseWorkday(LocalDate.of(2025, 7, 5), Duration.ofDays(1), true);
        assertEquals(LocalDate.of(2025, 7, 3), supposedStartDate);

        supposedStartDate = calendar.reverseWorkday(LocalDate.of(2025, 7, 6), Duration.ofDays(1), true);
        assertEquals(LocalDate.of(2025, 7, 3), supposedStartDate);

        // 区间计数落在非工作日上时向前推进
        supposedStartDate = calendar.reverseWorkday(LocalDate.of(2025, 7, 13), Duration.ofDays(4), true);
        assertEquals(LocalDate.of(2025, 7, 7), supposedStartDate);

        supposedStartDate = calendar.reverseWorkday(LocalDate.of(2025, 7, 13), Duration.ofDays(5), true);
        assertEquals(LocalDate.of(2025, 7, 4), supposedStartDate);
    }
}
