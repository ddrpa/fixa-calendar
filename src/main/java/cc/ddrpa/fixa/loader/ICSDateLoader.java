package cc.ddrpa.fixa.loader;

import biweekly.Biweekly;
import biweekly.component.VEvent;
import cc.ddrpa.fixa.FixaCalendar;
import cc.ddrpa.fixa.FixaCalendarException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
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

import static java.time.temporal.ChronoUnit.SECONDS;

/**
 * ICS 日历文件加载器，支持从 URL 或本地文件加载 ICS 格式的日历数据
 */
public class ICSDateLoader implements IFixaDateLoader {

    private static final Logger logger = LoggerFactory.getLogger(ICSDateLoader.class);

    private static final int SECONDS_IN_DAY = 24 * 60 * 60;

    private final URI calendarURI;
    private final File cacheFile;
    private final int cacheValidDays;
    private final int cacheValidDaysInDecJan;

    /**
     * 创建 ICS 日历加载器
     *
     * @param calendarURI          日历 ICS 文件的 URL
     * @param cacheFileName        本地缓存文件名
     * @param cacheValidDays       缓存有效期（天数）
     * @param cacheValidDaysInDecJan 12 月和 1 月的缓存有效期（天数），通常设置较短以便及时获取新年节假日
     */
    public ICSDateLoader(URI calendarURI, String cacheFileName, int cacheValidDays, int cacheValidDaysInDecJan) {
        this.calendarURI = calendarURI;
        this.cacheFile = new File(cacheFileName);
        this.cacheValidDays = cacheValidDays;
        this.cacheValidDaysInDecJan = cacheValidDaysInDecJan;
    }

    /**
     * 创建 ICS 日历加载器，使用默认缓存策略
     *
     * @param calendarURI   日历 ICS 文件的 URL
     * @param cacheFileName 本地缓存文件名
     */
    public ICSDateLoader(URI calendarURI, String cacheFileName) {
        this(calendarURI, cacheFileName, 300, 2);
    }

    @Override
    public boolean load(FixaCalendar calendarInstance) {
        if (!cacheFile.exists()) {
            try {
                downloadFile();
            } catch (IOException | InterruptedException e) {
                throw new FixaCalendarException("Failed to download ics file", e);
            }
        }
        try (FileInputStream fis = new FileInputStream(cacheFile)) {
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
        try (FileInputStream fis = new FileInputStream(cacheFile)) {
            process(singleDayEventFilter, multiDayEventFilter, calendarInstance, fis);
        } catch (IOException e) {
            throw new FixaCalendarException("Failed to read ics file", e);
        }
        return true;
    }

    @Override
    public boolean isOutdated() {
        if (!cacheFile.exists()) {
            return true;
        }
        LocalDateTime lastUpdateTime = LocalDateTime.ofEpochSecond(cacheFile.lastModified() / 1000L, 0,
                ZoneOffset.ofHours(8));
        LocalDateTime now = LocalDateTime.now();
        // 如果当前是 12 月或 1 月，使用较短的缓存有效期
        if (now.getMonth().getValue() == 1 || now.getMonth().getValue() == 12) {
            return now.minusDays(cacheValidDaysInDecJan).isAfter(lastUpdateTime);
        }
        return now.minusDays(cacheValidDays).isAfter(lastUpdateTime);
    }

    /**
     * 通过替换文件的方式更新日历数据，如果更新失败，则恢复旧文件，更新成功后删除旧文件
     */
    protected synchronized void updateFile() {
        File backupFile = new File(cacheFile.getName() + ".bak");
        // 将旧文件重命名
        if (cacheFile.exists()) {
            cacheFile.renameTo(backupFile);
        }
        try {
            downloadFile();
        } catch (IOException | InterruptedException e) {
            // 恢复旧文件
            if (backupFile.exists()) {
                backupFile.renameTo(cacheFile);
            }
            throw new FixaCalendarException("Failed to download file", e);
        }
        backupFile.delete();
    }

    /**
     * 下载日历数据
     *
     * @throws IOException
     * @throws InterruptedException
     */
    protected void downloadFile() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(calendarURI)
                .GET()
                .header("Accept", "text/calendar; charset=UTF-8")
                .header("Accept-Language", "en-US,en;q=0.5")
                .header("User-Agent",
                        "Mozilla/5.0 (compatible; FixaCalendar/1.0)")
                .header("Accept-Encoding", "gzip, deflate")
                .timeout(Duration.of(10, SECONDS))
                .build();
        HttpResponse<InputStream> response = HttpClient.newHttpClient()
                .send(request, BodyHandlers.ofInputStream());
        String encoding = response.headers().firstValue("Content-Encoding").orElse("");
        try (FileOutputStream fos = new FileOutputStream(cacheFile);
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
     * @param singleDayEventFilter 单日事件过滤器
     * @param multiDayEventFilter  多日事件过滤器
     * @param calendarInstance     日历实例
     * @param inputStream          ICS 文件输入流
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
