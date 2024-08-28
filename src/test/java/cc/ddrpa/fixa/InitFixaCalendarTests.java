package cc.ddrpa.fixa;

import static cc.ddrpa.fixa.TestCases.DATA_FLEXIBLE_WORKDAYS;
import static cc.ddrpa.fixa.TestCases.DATA_HOLIDAYS;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDate;
import org.junit.jupiter.api.RepeatedTest;

class InitFixaCalendarTests {

    private static final SecureRandom random = new SecureRandom();

    /**
     * 后定义的日期会覆盖先定义的日期
     * <p>
     * 例如先添加某个假期，然后将其标记为工作日，那么这个日期就会变成工作日
     */
    @RepeatedTest(10)
    void duplicateDateTest() {
        FixaCalendar calendar = new FixaCalendar(FixaWeekendEnum.SATURDAY_AND_SUNDAY,
            LocalDate.of(2024, 3, 9), Duration.ofDays(365));
        calendar.addFlexibleWorkdays(DATA_FLEXIBLE_WORKDAYS);
        calendar.addHolidays(DATA_HOLIDAYS);
        // turn random holidays into flexible workdays
        LocalDate thatDay = DATA_HOLIDAYS.get(random.nextInt(DATA_HOLIDAYS.size()));
        System.out.println("Turn " + thatDay + " into a flexible workday.");
        calendar.addFlexibleWorkday(thatDay);
        assertTrue(calendar.isWorkday(thatDay));
        // turn random flexible workdays into holidays
        thatDay = DATA_FLEXIBLE_WORKDAYS.get(random.nextInt(DATA_FLEXIBLE_WORKDAYS.size()));
        System.out.println("Turn " + thatDay + " into a holiday.");
        calendar.addHoliday(thatDay);
        assertFalse(calendar.isWorkday(thatDay));
    }

    /**
     * 生成一个周二和周三为周末的日历，测试生成的周末是否准确
     */
    @RepeatedTest(10)
    void initCalendarWithTuesdayAndWednesdayAsWeekendTest() {
        LocalDate randomStartDate = LocalDate.of(2024, 1 + random.nextInt(12),
            1 + random.nextInt(28));
        FixaCalendar calendar = new FixaCalendar(FixaWeekendEnum.TUESDAY_AND_WEDNESDAY,
            randomStartDate, Duration.ofDays(365 * 2));
        // 获取接下来某个时间段内的一天
        LocalDate thatDay = randomStartDate.plusDays(random.nextInt(365 * 2));
        // 计算接下来的 7 天，应当只有周二和周三被标记为周末
        for (int i = 0; i < 7; i++) {
            var theDay = thatDay.plusDays(i);
            var theDaysWeekday = theDay.getDayOfWeek().getValue();
            if (theDaysWeekday == 2 || theDaysWeekday == 3) {
                assertFalse(calendar.isWorkday(theDay));
            } else {
                assertTrue(calendar.isWorkday(theDay));
            }
        }
    }

    /**
     * 生成一个周五和周六为周末的日历，测试生成的周末是否准确
     */
    @RepeatedTest(10)
    void initCalendarWithFridayAndSaturdayAsWeekendTest() {
        LocalDate randomStartDate = LocalDate.of(2024, 1 + random.nextInt(12),
            1 + random.nextInt(28));
        FixaCalendar calendar = new FixaCalendar(FixaWeekendEnum.FRIDAY_AND_SATURDAY,
            randomStartDate, Duration.ofDays(365 * 2));
        // 获取接下来某个时间段内的一天
        LocalDate thatDay = randomStartDate.plusDays(random.nextInt(365 * 2 - 7));
        // 计算接下来的 7 天，应当只有周五和周六被标记为周末
        for (int i = 0; i < 7; i++) {
            var theDay = thatDay.plusDays(i);
            var theDaysWeekday = theDay.getDayOfWeek().getValue();
            if (theDaysWeekday == 5 || theDaysWeekday == 6) {
                assertFalse(calendar.isWorkday(theDay));
            } else {
                assertTrue(calendar.isWorkday(theDay));
            }
        }
    }

    /**
     * 生成一个周一为周末的日历，测试生成的周末是否准确
     */
    @RepeatedTest(10)
    void initCalendarWithMondayAsWeekendTest() {
        LocalDate randomStartDate = LocalDate.of(
            2024,
            1 + random.nextInt(12),
            1 + random.nextInt(28));
        FixaCalendar calendar = new FixaCalendar(FixaWeekendEnum.MONDAY_ONLY, randomStartDate,
            Duration.ofDays(365 * 2));
        // 获取接下来某个时间段内的一天
        LocalDate thatDay = randomStartDate.plusDays(random.nextInt(365 * 2 - 7));
        // 计算接下来的 7 天，应当只有周一被标记为周末
        for (int i = 0; i < 7; i++) {
            var theDay = thatDay.plusDays(i);
            var theDaysWeekday = theDay.getDayOfWeek().getValue();
            if (theDaysWeekday == 1) {
                assertFalse(calendar.isWorkday(theDay));
            } else {
                assertTrue(calendar.isWorkday(theDay));
            }
        }
    }

    /**
     * 生成一个周三为周末的日历，测试生成的周末是否准确
     */
    @RepeatedTest(10)
    void initCalendarWithWednesdayAsWeekendTest() {
        LocalDate randomStartDate = LocalDate.of(2024, 1 + random.nextInt(12),
            1 + random.nextInt(28));
        FixaCalendar calendar = new FixaCalendar(FixaWeekendEnum.WEDNESDAY_ONLY, randomStartDate,
            Duration.ofDays(365 * 2));
        // 获取接下来某个时间段内的一天
        LocalDate thatDay = randomStartDate.plusDays(random.nextInt(365 * 2 - 7));
        // 计算接下来的 7 天，应当只有周一被标记为周末
        for (int i = 0; i < 7; i++) {
            var theDay = thatDay.plusDays(i);
            var theDaysWeekday = theDay.getDayOfWeek().getValue();
            if (theDaysWeekday == 3) {
                assertFalse(calendar.isWorkday(theDay));
            } else {
                assertTrue(calendar.isWorkday(theDay));
            }
        }
    }
}