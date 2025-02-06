package cc.ddrpa.fixa;

import cc.ddrpa.fixa.loader.IFixaDateLoader;
import cc.ddrpa.fixa.loader.NopeDateLoader;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Objects;
import java.util.stream.IntStream;

public class FixaCalendarBuilder {

    private FixaWeekendEnum weekend = FixaWeekendEnum.SATURDAY_AND_SUNDAY;
    private LocalDate setWeekendAfter = LocalDate.now();
    private Duration duration = Duration.ofDays(365 * 5);
    private IFixaDateLoader loader;

    public FixaCalendarBuilder setWeekendType(FixaWeekendEnum weekend) {
        if (Objects.isNull(weekend)) {
            throw new NullPointerException("Weekend type cannot be null.");
        }
        this.weekend = weekend;
        return this;
    }

    public FixaCalendarBuilder startWeekendCalcAfter(LocalDate setWeekendAfter) {
        if (Objects.isNull(setWeekendAfter)) {
            throw new NullPointerException("Start date cannot be null.");
        }
        this.setWeekendAfter = setWeekendAfter;
        return this;
    }

    public FixaCalendarBuilder setWeekendCalcDuration(Duration duration) {
        if (Objects.isNull(duration)) {
            throw new NullPointerException("Duration cannot be null.");
        }
        if (duration.isNegative()) {
            throw new IllegalArgumentException("Negative duration is not supported right now.");
        }
        this.duration = duration;
        return this;
    }

    public FixaCalendarBuilder registerDateLoader(IFixaDateLoader loader) {
        this.loader = loader;
        return this;
    }

    public FixaCalendar build() {
        int durationInDays = Math.toIntExact(duration.toDays());
        int dayOfWeek = setWeekendAfter.getDayOfWeek().getValue();
        int startPos = Math.toIntExact(setWeekendAfter.toEpochDay());
        int[] weekendPos = new int[0];
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
        }
        return new FixaCalendar(weekendPos,
            Objects.isNull(loader) ? new NopeDateLoader() : loader);
    }
}