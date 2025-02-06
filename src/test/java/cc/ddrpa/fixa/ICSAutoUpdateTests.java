package cc.ddrpa.fixa;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cc.ddrpa.fixa.loader.AppleCalendarDateLoader;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * You can use @Scheduled annotation in Spring Boot project to schedule tasks.
 * <p>
 * Use @Scheduled(cron = "0 0 0 30 11 ?") to update holiday data every Christmas Day at midnight.
 */
class ICSAutoUpdateTests {

    private static final Logger logger = LoggerFactory.getLogger(ICSAutoUpdateTests.class);

    private static final FixaCalendar calendar = new FixaCalendarBuilder()
        .registerDateLoader(new AppleCalendarDateLoader())
        .build();

    private static final List<LocalDate> DATA_HOLIDAYS = List.of(
        LocalDate.of(2024, 1, 1),
        LocalDate.of(2024, 2, 10),
        LocalDate.of(2024, 2, 11),
        LocalDate.of(2024, 2, 12),
        LocalDate.of(2024, 2, 13),
        LocalDate.of(2024, 9, 15),
        LocalDate.of(2024, 9, 16),
        LocalDate.of(2024, 9, 17),
        LocalDate.of(2024, 10, 3));

    private static final List<LocalDate> DATA_FLEXIBLE_WORKDAYS = List.of(
        LocalDate.of(2024, 2, 4),
        LocalDate.of(2024, 2, 18),
        LocalDate.of(2024, 4, 7),
        LocalDate.of(2024, 9, 14),
        LocalDate.of(2024, 10, 12));

    @Test
    void parseTest() throws IOException {
        calendar.update(2025);
        for (var holiday : DATA_HOLIDAYS) {
            assertTrue(calendar.isHoliday(holiday), holiday + " is not a holiday");
            assertTrue(calendar.isDayOff(holiday), holiday + " is not a day off");
        }
        for (var workday : DATA_FLEXIBLE_WORKDAYS) {
            assertTrue(calendar.isFlexibleWorkday(workday), workday + " is not a flexible workday");
            assertFalse(calendar.isDayOff(workday), workday + " is a day off");
        }
    }
}