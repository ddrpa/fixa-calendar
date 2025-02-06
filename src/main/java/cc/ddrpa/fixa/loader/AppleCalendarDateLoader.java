package cc.ddrpa.fixa.loader;

import static java.time.temporal.ChronoUnit.SECONDS;

import biweekly.Biweekly;
import biweekly.component.VEvent;
import cc.ddrpa.fixa.FixaCalendar;
import cc.ddrpa.fixa.FixaCalendarException;
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
import java.util.ListIterator;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppleCalendarDateLoader implements IFixaDateLoader {

    private static final Logger logger = LoggerFactory.getLogger(AppleCalendarDateLoader.class);

    private static final int SECONDS_IN_DAY = 24 * 60 * 60;
    private static final File file = new File("apple-calendar.ics");

    private final URI calendarURI;

    public AppleCalendarDateLoader(URI calendarURI) {
        this.calendarURI = calendarURI;
    }

    public AppleCalendarDateLoader() {
        this(URI.create("https://calendars.icloud.com/holidays/cn_zh.ics/"));
    }

    @Override
    public boolean load(FixaCalendar calendarInstance) {
        if (!file.exists()) {
            try {
                downloadFile();
            } catch (URISyntaxException | IOException | InterruptedException e) {
                throw new FixaCalendarException("Failed to download ics file", e);
            }
        }
        try (FileInputStream fis = new FileInputStream(file)) {
            // 不按日期过滤
            process((ts) -> false, (ts, te) -> false, calendarInstance, fis);
        } catch (IOException e) {
            throw new FixaCalendarException("Failed to read ics file", e);
        }
        return true;
    }

    @Override
    public boolean update(int year, FixaCalendar calendarInstance) {
        updateFile();
        // 计算指定年度的 EpochSecond 上下限
        // 开始时间是指定年份的 1 月 1 日 0 时 0 分 0 秒
        long startEpoch = LocalDate.ofYearDay(year, 1)
            .atStartOfDay(ZoneOffset.ofHours(8))
            .toInstant()
            .toEpochMilli() / 1000L;
        // 结束时间是指定年份的 12 月 31 日 23 时 59 分 59 秒
        long endEpoch = LocalDate.ofYearDay(year + 1, 1)
            .atStartOfDay(ZoneOffset.ofHours(8))
            .toInstant()
            .toEpochMilli() / 1000L - 1L;
        // 过滤掉不在指定年度内的单日日历事件
        Function<Long, Boolean> singleDayEventFilter = (ts) -> ts < startEpoch || ts > endEpoch;
        // 过滤掉与指定年度无交集的多日日历事件
        BiFunction<Long, Long, Boolean> multiDayEventFilter = (ts, te) -> te < startEpoch
            || ts > endEpoch;
        try (FileInputStream fis = new FileInputStream(file)) {
            process(singleDayEventFilter, multiDayEventFilter, calendarInstance, fis);
        } catch (IOException e) {
            throw new FixaCalendarException("Failed to read ics file", e);
        }
        return true;
    }

    @Override
    public boolean isOutdated() {
        LocalDateTime lastUpdateTime = LocalDateTime.ofEpochSecond(file.lastModified(), 0,
            ZoneOffset.ofHours(8));
        LocalDateTime now = LocalDateTime.now();
        // 如果当前是 12-1月，加速过期
        if (now.getMonth().getValue() == 1 || now.getMonth().getValue() == 12) {
            return now.minusDays(2L).isAfter(lastUpdateTime);
        }
        return now.minusDays(300L).isAfter(lastUpdateTime);
    }

    /**
     * 通过替换文件的方式更新日历数据，如果更新失败，则恢复旧文件，更新成功后删除旧文件
     */
    protected synchronized void updateFile() {
        File backupFile = new File(file.getName() + ".bak");
        // 将旧文件重命名
        file.renameTo(backupFile);
        try {
            downloadFile();
        } catch (URISyntaxException | IOException | InterruptedException e) {
            // 恢复旧文件
            backupFile.renameTo(file);
            throw new FixaCalendarException("Failed to download file", e);
        }
        backupFile.delete();
    }

    /**
     * 下载日历数据
     *
     * @throws URISyntaxException
     * @throws IOException
     * @throws InterruptedException
     */
    protected void downloadFile() throws URISyntaxException, IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(calendarURI)
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

    /**
     * 将解析出的所有日历事件添加到日历实例
     *
     * @param singleDayEventFilter
     * @param multiDayEventFilter
     * @param calendarInstance
     * @param inputStream
     * @throws IOException
     */
    protected void process(
        Function<Long, Boolean> singleDayEventFilter,
        BiFunction<Long, Long, Boolean> multiDayEventFilter,
        FixaCalendar calendarInstance,
        InputStream inputStream
    ) throws IOException {
        List<VEvent> eventList = Biweekly.parse(inputStream).first().getComponents(VEvent.class);
        // 以倒序遍历，这样更近的日期会先读取，因为更新动作一般只关注明年或今年的数据
        ListIterator<VEvent> iterator = eventList.listIterator(eventList.size());
        while (iterator.hasPrevious()) {
            VEvent event = iterator.previous();
            // 将日历事件的起始时间转换为秒数时间戳，即该日的 0 时 0 分 0 秒
            long timeStart = event.getDateStart().getValue().getTime() / 1000L;
            long timeEnd = -1;
            boolean isSingleDayEvent;
            if (Objects.isNull(event.getDateEnd())) {
                // 单日事件没有 dateEnd
                if (singleDayEventFilter.apply(timeStart)) {
                    continue;
                }
                isSingleDayEvent = true;
            } else {
                // 持续多日的日历事件，会有 dateEnd
                timeEnd = event.getDateEnd().getValue().getTime() / 1000L - SECONDS_IN_DAY;
                if (multiDayEventFilter.apply(timeStart, timeEnd)) {
                    continue;
                }
                isSingleDayEvent = false;
            }
            // FEAT_NEED 在更新指定年份时，如果读取到关注年度之前的日历事件，可以终止读取
            // 获取日历事件名称
            String summary = event.getSummary().getValue();
            boolean isHolidayEvent;
            // 判断日历事件是否是节假日或调休
            if (summary.contains("休")) {
                isHolidayEvent = true;
            } else if (summary.contains("班")) {
                isHolidayEvent = false;
            } else {
                continue;
            }
            LocalDate eventDataStart = LocalDateTime.ofEpochSecond(timeStart, 0,
                    ZoneOffset.ofHours(8))
                .toLocalDate();
            if (isSingleDayEvent) {
                if (isHolidayEvent) {
                    calendarInstance.addHoliday(eventDataStart);
                } else {
                    calendarInstance.addFlexibleWorkday(eventDataStart);
                }
            } else {
                LocalDate eventDataEnd = LocalDateTime.ofEpochSecond(timeEnd, 0,
                    ZoneOffset.ofHours(8)).toLocalDate();
                if (isHolidayEvent) {
                    calendarInstance.addHolidays(eventDataStart, eventDataEnd);
                } else {
                    calendarInstance.addFlexibleWorkdays(eventDataStart, eventDataEnd);
                }
            }
        }
    }
}