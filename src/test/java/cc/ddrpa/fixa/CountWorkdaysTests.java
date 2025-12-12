package cc.ddrpa.fixa;

import cc.ddrpa.fixa.loader.ICSDateLoader;

import java.net.URI;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.*;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CountWorkdaysTests {

    private static final Logger logger = LoggerFactory.getLogger(CountWorkdaysTests.class);
    private static final FixaCalendar calendar = new FixaCalendarBuilder()
            .setWeekendType(FixaWeekendEnum.SATURDAY_AND_SUNDAY)
            .startWeekendCalcAfter(LocalDate.of(2025, 1, 1))
            .registerDateLoader(new ICSDateLoader(
                    URI.create("https://calendars.icloud.com/holidays/cn_zh.ics/"),
                    "holiday-calendar.ics"))
            .build();

    @Test
    void nextDayOffTest() {
        LocalDate day = calendar.workday(LocalDate.of(2025, 4, 21), Duration.parse("P5D"));
        logger.info(day.toString());
        ZonedDateTime zonedDateTime = ZonedDateTime.of(day, LocalTime.MAX, ZoneId.of("UTC+8"));
        logger.info("ZonedDateTime: {}", zonedDateTime);
        String timeAsString = zonedDateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        logger.info("Formatted ZonedDateTime: {}", timeAsString);
    }

    @Test
    void isOffdayTest() {
        LocalDate date = LocalDate.of(2025, 4, 26);
        assertTrue(calendar.isDayOff(date), date + " should be a day off");
        assertFalse(calendar.isWorkday(date), date + " should not be a day off");
    }
}