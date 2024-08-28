package cc.ddrpa.fixa;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import biweekly.Biweekly;
import biweekly.component.VEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;
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

    private static final FixaCalendar calendar = new FixaCalendar(
        FixaWeekendEnum.SATURDAY_AND_SUNDAY, LocalDate.of(2023, 12, 25), Duration.ofDays(400));

    private static final List<LocalDate> DATA_HOLIDAYS = List.of(
        LocalDate.of(2024, 1, 1),
        LocalDate.of(2024, 2, 10),
        LocalDate.of(2024, 2, 11),
        LocalDate.of(2024, 2, 12),
        LocalDate.of(2024, 2, 13),
        LocalDate.of(2024, 10, 3));

    private static final List<LocalDate> DATA_FLEXIBLE_WORKDAYS = List.of(
        LocalDate.of(2024, 2, 4),
        LocalDate.of(2024, 2, 18),
        LocalDate.of(2024, 4, 7),
        LocalDate.of(2024, 10, 12));

    @Test
    void downloadCalendarFileTest() throws URISyntaxException, IOException, InterruptedException {
        URI icsSubscribeURI = new URI(
            String.format("https://calendars.icloud.com/holidays/cn_zh.ics/"));
        downloadICSFile(icsSubscribeURI, new File("2024.ics"));
    }

    @Test
    void parseTest() throws IOException {
        parseAppleCalendar(2024);

        for (var holiday : DATA_HOLIDAYS) {
            assertTrue(calendar.isHoliday(holiday));
            assertTrue(calendar.isDayOff(holiday));
        }
        for (var workday : DATA_FLEXIBLE_WORKDAYS) {
            assertTrue(calendar.isFlexibleWorkday(workday));
            assertFalse(calendar.isDayOff(workday));
        }
    }

    private void parseAppleCalendar(int year) throws IOException {
        // 计算本年度的 EpochSecond 上下限
        long startEpochMilli = LocalDate.ofYearDay(year, 1)
            .atStartOfDay(ZoneOffset.ofHours(8))
            .toInstant()
            .toEpochMilli();
        long endEpochMilli = LocalDate.ofYearDay(year + 1, 1)
            .atStartOfDay(ZoneOffset.ofHours(8))
            .minusSeconds(1)
            .toInstant()
            .toEpochMilli();
        try (FileInputStream fis = new FileInputStream(year + ".ics")) {
            List<VEvent> eventList = Biweekly.parse(fis).first().getComponents(VEvent.class);
            for (VEvent event : eventList) {
                // 判断日历事件是否是节假日或调休
                String summary = event.getSummary().getValue();
                boolean isHoliday;
                if (summary.contains("休")) {
                    // 休假
                    isHoliday = true;
                    logger.info("休假: {}", summary);
                } else if (summary.contains("班")) {
                    // 调休
                    isHoliday = false;
                    logger.info("调休: {}", summary);
                } else {
                    continue;
                }
                if (Objects.isNull(event.getDateEnd())) {
                    // 一天的日历事件，没有 dateEnd
                    long timeStart = event.getDateStart().getValue().getTime();
                    if (timeStart < startEpochMilli || timeStart > endEpochMilli) {
                        // 不在指定年度内，跳过
                        continue;
                    }
                    LocalDate dataStart = LocalDateTime.ofEpochSecond(timeStart / 1000, 0, ZoneOffset.ofHours(8))
                        .toLocalDate();
                    if (isHoliday) {
                        calendar.addHoliday(dataStart);
                    } else {
                        calendar.addFlexibleWorkday(dataStart);
                    }
                    logger.info("dataStart: {}", dataStart);
                } else {
                    // 持续多日的日历事件，会有 dateEnd
                    // 确保 dataEnd 在本年度内或之后
                    long timeEnd = event.getDateEnd().getValue().getTime();
                    if (timeEnd < startEpochMilli) {
                        continue;
                    } else if (timeEnd > endEpochMilli) {
                        // 把事件日期收束到本年度内
                        timeEnd = endEpochMilli;
                    }
                    // 确保 dataStart 在本年度内或之前
                    long timeStart = event.getDateStart().getValue().getTime();
                    if (timeStart > endEpochMilli) {
                        continue;
                    } else if (timeStart < startEpochMilli) {
                        // 把事件日期收束到本年度内
                        timeStart = startEpochMilli;
                    }
                    LocalDate dataStart = LocalDateTime.ofEpochSecond(timeStart / 1000, 0,
                        ZoneOffset.ofHours(8)).toLocalDate();
                    LocalDate dataEnd = LocalDateTime.ofEpochSecond(timeEnd / 1000, 0,
                        ZoneOffset.ofHours(8)).toLocalDate();
                    if (isHoliday) {
                        calendar.addHolidays(dataStart, dataEnd);
                    } else {
                        calendar.addFlexibleWorkdays(dataStart, dataEnd);
                    }
                    logger.info("dataStart: {}, dataEnd: {}", dataStart, dataEnd);
                }
            }
        }
    }

    private void downloadICSFile(URI uri, File file)
        throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(uri)
            .GET()
            .header("Accept", "text/calendar; charset=UTF-8")
            .header("Accept-Language", "en-US,en;q=0.5")
            .header("User-Agent",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:129.0) Gecko/20100101 Firefox/129.0")
            .header("Accept-Encoding", "gzip, deflate")
            .timeout(Duration.of(3, SECONDS))
            .build();
        HttpResponse<InputStream> response = HttpClient.newHttpClient()
            .send(request, BodyHandlers.ofInputStream());
        String encoding = response.headers().firstValue("Content-Encoding").orElse("");
        try (FileOutputStream fos = new FileOutputStream(file);
            InputStream is = response.body()) {
            if ("gzip".equalsIgnoreCase(encoding)) {
                try (GZIPInputStream gis = new GZIPInputStream(is)) {
                    fos.write(gis.readAllBytes());
                    fos.flush();
                }
            } else if ("deflate".equalsIgnoreCase(encoding)) {
                try (InflaterInputStream iis = new InflaterInputStream(is)) {
                    fos.write(iis.readAllBytes());
                    fos.flush();
                }
            } else {
                fos.write(is.readAllBytes());
                fos.flush();
            }
        }
    }
}