package cc.ddrpa.fixa;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;
import org.roaringbitmap.IntConsumer;
import org.roaringbitmap.RoaringBitmap;

/**
 * FixaCalendar was mainly used to calculate workdays and holidays.
 */
public class FixaCalendar {

    // 存储周末
    private final RoaringBitmap weekendMap = RoaringBitmap.bitmapOf();
    // 存储节日
    private final RoaringBitmap holidayMap = RoaringBitmap.bitmapOf();
    // 存储调休产生的工作日
    private final RoaringBitmap flexibleWorkdayMap = RoaringBitmap.bitmapOf();
    // 非工作日计算方法
    // day-off = weekend + holiday - flexibleWorkday
    private final RoaringBitmap dayOffMap = RoaringBitmap.bitmapOf();

    /**
     * Construct a new FixaCalendar with default settings
     * <ul>
     * <li>Weekend means Saturday and Sunday.</li>
     * <li>Use last Saturday for weekend calculation.</li>
     * <li>Calculate the next 5 years' weekends.</li>
     * </ul>
     * <p>
     * 使用默认设置构造一个新的 FixaCalendar，具有如下行为
     * <ul>
     *     <li>周末为周六和周日</li>
     *     <li>使用上周六作为周末计算的起始日期</li>
     *     <li>计算接下来 5 年的周末</li>
     * </ul>
     */
    public FixaCalendar() {
        new FixaCalendar(FixaWeekendEnum.SATURDAY_AND_SUNDAY, LocalDate.now(),
            Duration.ofDays(365 * 5));
    }

    /**
     * Construct a new FixaCalendar with given settings
     * <p>
     * 构造一个新的 FixaCalendar
     *
     * @param weekend         weekend type
     * @param setWeekendAfter use this date as the starting date for weekend calculation
     * @param duration        calculate the weekends for the next duration days
     */
    public FixaCalendar(FixaWeekendEnum weekend, LocalDate setWeekendAfter, Duration duration) {
        if (duration.isNegative()) {
            throw new IllegalArgumentException("Negative duration is not supported right now.");
        }
        int durationInDays = Math.toIntExact(duration.toDays());
        int dayOfWeek = setWeekendAfter.getDayOfWeek().getValue();
        int startPos = Math.toIntExact(setWeekendAfter.toEpochDay());
        int[] weekendPos;
        if (weekend.isSingleDayWeekend()) {
            startPos = startPos - dayOfWeek - 4 + weekend.getCode();
            weekendPos = IntStream.iterate(startPos, i -> i + 7)
                .limit(durationInDays / 7)
                .toArray();
        } else if (weekend.isDoubleDayWeekend()) {
            startPos = startPos - dayOfWeek - 2 + weekend.getCode();
            weekendPos = IntStream.iterate(startPos, i -> i + 7)
                .limit(durationInDays / 7)
                .flatMap(i -> IntStream.of(i, i + 1))
                .toArray();
        } else {
            throw new IllegalArgumentException("Invalid weekend type.");
        }
        weekendMap.add(weekendPos);
        dayOffMap.add(weekendPos);
    }

    /**
     * whether given date is a workday
     * <p>
     * 判定给定日期是否是工作日
     *
     * @param date date to be checked
     * @return true if it's a workday
     */
    public boolean isWorkday(LocalDate date) {
        return !dayOffMap.contains(Math.toIntExact(date.toEpochDay()));
    }

    /**
     * whether given date is a day-off
     * <p>
     * 判定给定日期是否是非工作日（包括周末和节假日）
     *
     * @param date date to be checked
     * @return true if it's a day-off
     */
    public boolean isDayOff(LocalDate date) {
        return dayOffMap.contains(Math.toIntExact(date.toEpochDay()));
    }

    /**
     * whether given date is a weekend
     * <p>
     * 判定给定日期是否是周末
     *
     * @param date
     * @return
     */
    public boolean isWeekend(LocalDate date) {
        return weekendMap.contains(Math.toIntExact(date.toEpochDay()));
    }

    /**
     * whether given date is a holiday
     * <p>
     * 判定给定日期是否是节假日
     *
     * @param date
     * @return
     */
    public boolean isHoliday(LocalDate date) {
        return holidayMap.contains(Math.toIntExact(date.toEpochDay()));
    }

    /**
     * whether given date is a flexible workday
     * <p>
     * 判定给定日期是否是调休产生的工作日
     *
     * @param date
     * @return
     */
    public boolean isFlexibleWorkday(LocalDate date) {
        return flexibleWorkdayMap.contains(Math.toIntExact(date.toEpochDay()));
    }

    /**
     * Returns the number of whole working days between startDate and endDate
     * <p>
     * Should behaves like <a
     * href="https://support.microsoft.com/en-us/office/networkdays-function-48e717bf-a7a3-495f-969e-5005e3eb18e7">
     * NETWORKDAYS(start_date, end_date) function in Microsoft Excel</a>
     * <p>
     * 返回给定日期范围内的工作日数量（包括开始和结束日期），行为应该类似于 Microsoft Excel 中的 <a
     * href="https://support.microsoft.com/en-us/office/networkdays-function-48e717bf-a7a3-495f-969e-5005e3eb18e7">
     * NETWORKDAYS(start_date, end_date)</a> 函数
     *
     * @param startDate start date(included)
     * @param endDate   end date(included)
     * @return the number of whole working days between
     */
    public int netWorkdays(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("StartDate should be before endDate");
        }
        long startPos = startDate.toEpochDay();
        long endPos = endDate.toEpochDay() + 1;
        long cardinality = dayOffMap.rangeCardinality(startPos, endPos);
        return Math.toIntExact(endPos - startPos - cardinality);
    }

    /**
     * Returns the number of working days during given days after the starting date
     * <p>
     * 返回给定日期范围内的工作日数量
     *
     * @param startDate start date(included)
     * @param duration  represent the number of workday and non-workday days after start_date
     * @return the number of working days
     */
    public int netWorkdays(LocalDate startDate, Duration duration) {
        if (duration.isNegative()) {
            throw new IllegalArgumentException("Negative duration is not supported right now.");
        }
        return netWorkdays(startDate, startDate.plusDays(duration.toDays()));
    }

    /**
     * Returns a date that is the indicated number of working days after the starting date
     * <p>
     * Should behaves like <a
     * href="https://support.microsoft.com/en-us/office/workday-function-f764a5b7-05fc-4494-9486-60d494efbf33">
     * WORKDAY(start_date, end_date) function in Microsoft Excel</a>
     * <p>
     * 返回给定日期之后的第 n 个工作日，行为应该类似于 Microsoft Excel 中的 <a
     * href="https://support.microsoft.com/en-us/office/workday-function-f764a5b7-05fc-4494-9486-60d494efbf33">
     * WORKDAY(start_date, end_date)</a> 函数
     *
     * @param startDate start date
     * @param duration  represent the number of non-weekend and non-holiday days after start_date.
     *                  negative value is not supported for now.
     * @return the date
     */
    public LocalDate workday(LocalDate startDate, Duration duration) {
        long length = duration.toDays();
        long startPos = startDate.toEpochDay();
        long possibleEndPos = startPos + length;
        // RoaringBitmap::rangeCardinality calculate cardinality between [start, end)
        // by adding 1 to both start and end, we can calculate the cardinality between (start, end]
        long cardinality = dayOffMap.rangeCardinality(startPos + 1, possibleEndPos + 1);
        while (cardinality != 0) {
            // if there are values between (start, end], move the startPos to the end of the range
            startPos = possibleEndPos;
            possibleEndPos += cardinality;
            // re-calculate the cardinality between (new-start, new-end]
            cardinality = dayOffMap.rangeCardinality(startPos + 1, possibleEndPos + 1);
        }
        return LocalDate.ofEpochDay(possibleEndPos);
    }

    /**
     * Return list of holidays between startDate and given duration
     * <p>
     * 给定起始日期和一个持续时间，返回在这段时间内的非工作日列表
     *
     * @param startDate start date(included)
     * @param duration
     * @return
     */
    public List<LocalDate> dayOffs(LocalDate startDate, Duration duration) {
        if (duration.isNegative()) {
            throw new IllegalArgumentException("Negative duration is not supported right now.");
        }
        int startPos = Math.toIntExact(startDate.toEpochDay());
        int length = Math.toIntExact(duration.toDays());
        List<Integer> bits = new ArrayList<>(length);
        // RoaringBitmap::forEachInRange calculate retrieve values between [start, end)
        // get [start, end] by using [start, end + 1)
        dayOffMap.forEachInRange(startPos, length + 1, new FixaDateConsumer(bits));
        return bits.stream()
            .map(LocalDate::ofEpochDay)
            .collect(Collectors.toList());
    }

    /**
     * Return list of day-off between startDate and endDate
     * <p>
     * 给定起始日期和结束日期，返回在这段时间内的非工作日列表
     *
     * @param startDate start date(included)
     * @param endDate   end date(included)
     * @return
     */
    public List<LocalDate> dayOffs(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("StartDate should be before endDate");
        }
        int startPos = Math.toIntExact(startDate.toEpochDay());
        int length = Math.toIntExact(endDate.toEpochDay()) + 1 - startPos;
        List<Integer> bits = new ArrayList<>(length);
        dayOffMap.forEachInRange(startPos, length, new FixaDateConsumer(bits));
        return bits.stream()
            .map(LocalDate::ofEpochDay)
            .collect(Collectors.toList());
    }

    /**
     * Get the next day-off after the given date
     * <p>
     * 给定一个日期，返回下一个非工作日（当日不计）
     *
     * @param startDate start date(not include)
     * @return
     */
    public LocalDate nextDayOff(LocalDate startDate) {
        return LocalDate.ofEpochDay(
            dayOffMap.nextValue(Math.toIntExact(startDate.toEpochDay()) + 1));
    }

    /**
     * Add holidays that are recurring
     * <p>
     * 添加周期性的节假日
     *
     * @param startDate first holiday
     * @param interval  interval between holidays
     * @param loop      how many holidays need to add
     */
    public void addRecurringHolidays(LocalDate startDate, int interval, int loop) {
        int startPos = Math.toIntExact(startDate.toEpochDay());
        int[] bits = IntStream.range(0, loop)
            .map(i -> startPos + i * interval)
            .toArray();
        holidayMap.add(bits);
        dayOffMap.add(bits);
    }

    /**
     * Add single date as holiday
     * <p>
     * 添加单个日期作为节假日
     *
     * @param date date to add
     */
    public void addHoliday(LocalDate date) {
        int pos = Math.toIntExact(date.toEpochDay());
        holidayMap.add(pos);
        dayOffMap.add(pos);
    }

    /**
     * Add multiple dates as holidays
     * <p>
     * 使用开始日期和结束日期添加多个节假日
     *
     * @param firstDay first day of holiday
     * @param lastDay  last day of holiday
     */
    public void addHolidays(LocalDate firstDay, LocalDate lastDay) {
        int[] bits = IntStream.rangeClosed(
                Math.toIntExact(firstDay.toEpochDay()),
                Math.toIntExact(lastDay.toEpochDay()))
            .toArray();
        holidayMap.add(bits);
        dayOffMap.add(bits);
    }

    /**
     * Add multiple dates as holidays
     * <p>
     * 添加多个节假日
     *
     * @param dates dates to add
     */
    public void addHolidays(Iterable<LocalDate> dates) {
        int[] bits = StreamSupport.stream(dates.spliterator(), false)
            .map(LocalDate::toEpochDay)
            .mapToInt(Math::toIntExact)
            .sorted()
            .toArray();
        holidayMap.add(bits);
        dayOffMap.add(bits);
    }

    /**
     * Add single date as flexible workday
     * <p>
     * if the date is neither weekend nor holiday, nothing should be different
     * <p>
     * 添加单个日期作为调休产生的工作日
     * <p>
     * 如果给定日期既不是周末也不是节假日，不会有任何变化
     *
     * @param date date to add
     */
    public void addFlexibleWorkday(LocalDate date) {
        int pos = Math.toIntExact(date.toEpochDay());
        flexibleWorkdayMap.add(pos);
        dayOffMap.remove(pos);
    }

    /**
     * Add multiple dates as flexible workdays
     * <p>
     * 使用开始日期和结束日期添加多个调休产生的工作日
     *
     * @param firstDay first day of flexible workday
     * @param lastDay  last day of flexible workday
     */
    public void addFlexibleWorkdays(LocalDate firstDay, LocalDate lastDay) {
        int[] bits = IntStream.rangeClosed(
                Math.toIntExact(firstDay.toEpochDay()),
                Math.toIntExact(lastDay.toEpochDay()))
            .toArray();
        flexibleWorkdayMap.add(bits);
        dayOffMap.andNot(flexibleWorkdayMap);
    }

    /**
     * Add multiple dates as flexible workdays
     * <p>
     * 添加多个调休产生的工作日
     *
     * @param dates
     */
    public void addFlexibleWorkdays(Iterable<LocalDate> dates) {
        int[] bits = StreamSupport.stream(dates.spliterator(), false)
            .map(LocalDate::toEpochDay)
            .mapToInt(Math::toIntExact)
            .sorted()
            .toArray();
        flexibleWorkdayMap.add(bits);
        dayOffMap.andNot(flexibleWorkdayMap);
    }

    /**
     * Return a {@link RoaringBitmap} copy of the dayOffMap
     * <p>
     * 返回实际存储非工作日信息的 {@link RoaringBitmap} 副本
     *
     * @return
     */
    public RoaringBitmap rawDayOffMapClone() {
        return dayOffMap.clone();
    }

    private final class FixaDateConsumer implements IntConsumer {

        List<Integer> presentDates;

        public FixaDateConsumer(List<Integer> presentDates) {
            this.presentDates = presentDates;
        }

        @Override
        public void accept(int value) {
            presentDates.add(value);
        }
    }
}