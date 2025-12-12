package cc.ddrpa.fixa;

import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import static cc.ddrpa.fixa.TestCases.DATA_FLEXIBLE_WORKDAYS;
import static cc.ddrpa.fixa.TestCases.DATA_HOLIDAYS;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test cases for FixaCalendar workdays calculation.
 */
public class WorkdayTests {
    private static final FixaCalendar calendar = new FixaCalendar(FixaWeekendEnum.SATURDAY_AND_SUNDAY, LocalDate.of(2024, 3, 9), Duration.ofDays(365));
    private static final List<ImmutableTriple<LocalDate, LocalDate, Integer>> NETWORKDAY_TESTCASE = List.of(
            // 工作日开始，非工作日结束
            ImmutableTriple.of(LocalDate.of(2024, 3, 22), LocalDate.of(2024, 3, 23), 1),
            ImmutableTriple.of(LocalDate.of(2024, 4, 18), LocalDate.of(2024, 4, 27), 4),
            // 工作日开始，工作日结束
            ImmutableTriple.of(LocalDate.of(2024, 3, 22), LocalDate.of(2024, 3, 25), 2),
            ImmutableTriple.of(LocalDate.of(2024, 4, 18), LocalDate.of(2024, 4, 26), 4),
            // 工作日开始，调休工作日结束
            ImmutableTriple.of(LocalDate.of(2024, 3, 22), LocalDate.of(2024, 3, 30), 7),
            ImmutableTriple.of(LocalDate.of(2024, 4, 18), LocalDate.of(2024, 5, 4), 8),
            // 非工作日开始，工作日结束
            ImmutableTriple.of(LocalDate.of(2024, 3, 23), LocalDate.of(2024, 3, 26), 2),
            ImmutableTriple.of(LocalDate.of(2024, 4, 21), LocalDate.of(2024, 4, 26), 1),
            // 非工作日开始，非工作日结束
            ImmutableTriple.of(LocalDate.of(2024, 4, 7), LocalDate.of(2024, 4, 13), 5),
            ImmutableTriple.of(LocalDate.of(2024, 4, 21), LocalDate.of(2024, 4, 27), 1),
            // 非工作日开始，调休工作日结束
            ImmutableTriple.of(LocalDate.of(2024, 3, 23), LocalDate.of(2024, 3, 31), 7),
            ImmutableTriple.of(LocalDate.of(2024, 4, 21), LocalDate.of(2024, 5, 4), 5),
            // 调休工作日开始，工作日结束
            ImmutableTriple.of(LocalDate.of(2024, 3, 30), LocalDate.of(2024, 4, 5), 7),
            ImmutableTriple.of(LocalDate.of(2024, 4, 19), LocalDate.of(2024, 4, 27), 3),
            // 调休工作日开始，非工作日结束
            ImmutableTriple.of(LocalDate.of(2024, 3, 31), LocalDate.of(2024, 4, 6), 6),
            ImmutableTriple.of(LocalDate.of(2024, 4, 19), LocalDate.of(2024, 4, 27), 3),
            // 调休工作日开始，调休工作日结束
            ImmutableTriple.of(LocalDate.of(2024, 3, 30), LocalDate.of(2024, 4, 19), 17),
            ImmutableTriple.of(LocalDate.of(2024, 4, 19), LocalDate.of(2024, 5, 4), 7)
    );
    private static final List<ImmutableTriple<LocalDate, Integer, LocalDate>> WORKDAY_TESTCASE = List.of(
            // 工作日开始
            ImmutableTriple.of(LocalDate.of(2024, 3, 22), 1, LocalDate.of(2024, 3, 25)),
            ImmutableTriple.of(LocalDate.of(2024, 3, 22), 7, LocalDate.of(2024, 3, 31)),
            ImmutableTriple.of(LocalDate.of(2024, 4, 18), 1, LocalDate.of(2024, 4, 19)),
            ImmutableTriple.of(LocalDate.of(2024, 4, 18), 7, LocalDate.of(2024, 5, 4)),
            // 非工作日开始
            ImmutableTriple.of(LocalDate.of(2024, 3, 23), 1, LocalDate.of(2024, 3, 25)),
            ImmutableTriple.of(LocalDate.of(2024, 3, 23), 7, LocalDate.of(2024, 3, 31)),
            ImmutableTriple.of(LocalDate.of(2024, 4, 21), 1, LocalDate.of(2024, 4, 26)),
            ImmutableTriple.of(LocalDate.of(2024, 4, 21), 7, LocalDate.of(2024, 5, 7)),
            // 调休工作日开始，工作日结束
            ImmutableTriple.of(LocalDate.of(2024, 3, 30), 1, LocalDate.of(2024, 3, 31)),
            ImmutableTriple.of(LocalDate.of(2024, 3, 30), 7, LocalDate.of(2024, 4, 8)),
            ImmutableTriple.of(LocalDate.of(2024, 4, 20), 1, LocalDate.of(2024, 4, 26)),
            ImmutableTriple.of(LocalDate.of(2024, 4, 20), 7, LocalDate.of(2024, 5, 7))
    );

    @BeforeAll
    static void setup() {
        calendar.addHolidays(DATA_HOLIDAYS);
        calendar.addFlexibleWorkdays(DATA_FLEXIBLE_WORKDAYS);
    }

    @Test
    void netWorkdaysTest() {
        NETWORKDAY_TESTCASE.forEach(testCase -> {
            var start = testCase.getLeft();
            var end = testCase.getMiddle();
            assertEquals(testCase.getRight(), calendar.netWorkdays(start, end));
        });
    }

    @Test
    void workdayTest() {
        WORKDAY_TESTCASE.forEach(testCase -> {
            var start = testCase.getLeft();
            var duration = Duration.ofDays(testCase.getMiddle());
            assertEquals(testCase.getRight(), calendar.workday(start, duration));
        });
    }
}