package cc.ddrpa.fixa;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import static cc.ddrpa.fixa.TestCases.DATA_FLEXIBLE_WORKDAYS;
import static cc.ddrpa.fixa.TestCases.DATA_HOLIDAYS;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class HolidayTests {
    private static final FixaCalendar calendar = new FixaCalendar(FixaWeekendEnum.SATURDAY_AND_SUNDAY, LocalDate.of(2024, 3, 9), Duration.ofDays(365));
    private static final List<ImmutableTriple<LocalDate, LocalDate, Integer>> DAYOFF_TESTCASE = List.of(
            // 节假日开始，节假日结束
            ImmutableTriple.of(LocalDate.of(2024, 4, 22), LocalDate.of(2024, 4, 25), 4),
            ImmutableTriple.of(LocalDate.of(2024, 5, 1), LocalDate.of(2024, 5, 2), 2),
            // 节假日开始，非节假日结束
            ImmutableTriple.of(LocalDate.of(2024, 4, 22), LocalDate.of(2024, 4, 26), 4),
            ImmutableTriple.of(LocalDate.of(2024, 5, 1), LocalDate.of(2024, 5, 3), 2),
            // 非节假日开始，节假日结束
            ImmutableTriple.of(LocalDate.of(2024, 4, 19), LocalDate.of(2024, 4, 23), 3),
            ImmutableTriple.of(LocalDate.of(2024, 4, 30), LocalDate.of(2024, 5, 2), 2),
            // 非节假日开始，非节假日结束
            ImmutableTriple.of(LocalDate.of(2024, 4, 19), LocalDate.of(2024, 4, 26), 5),
            ImmutableTriple.of(LocalDate.of(2024, 4, 30), LocalDate.of(2024, 5, 4), 2)
    );
    private static final List<ImmutablePair<LocalDate, LocalDate>> NEXT_DAYOFF_TESTCASE = List.of(
            ImmutablePair.of(LocalDate.of(2024, 4, 21), LocalDate.of(2024, 4, 22)),
            // 自身是节假日
            ImmutablePair.of(LocalDate.of(2024, 4, 22), LocalDate.of(2024, 4, 23)),
            ImmutablePair.of(LocalDate.of(2024, 4, 25), LocalDate.of(2024, 4, 27)),
            // 下一个为周末
            ImmutablePair.of(LocalDate.of(2024, 4, 26), LocalDate.of(2024, 4, 27)),
            // 避开调休产生的工作日
            ImmutablePair.of(LocalDate.of(2024, 5, 3), LocalDate.of(2024, 5, 5))
    );

    @BeforeAll
    static void setup() {
        calendar.addHolidays(DATA_HOLIDAYS);
        calendar.addFlexibleWorkdays(DATA_FLEXIBLE_WORKDAYS);
    }

    @Test
    void dayOffTest() {
        DAYOFF_TESTCASE.forEach(testCase -> {
            var startDate = testCase.getLeft();
            var endDate = testCase.getMiddle();
            assertEquals(testCase.getRight(), calendar.dayOffs(startDate, endDate).size());
        });
    }

    @Test
    void nextDayOffTest() {
        NEXT_DAYOFF_TESTCASE.forEach(testCase -> assertEquals(testCase.getRight(), calendar.nextDayOff(testCase.getLeft())));
    }
}