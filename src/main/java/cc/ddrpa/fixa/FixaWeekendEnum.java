package cc.ddrpa.fixa;

import java.util.stream.Stream;

/**
 * Indicates the weekend day type.
 * <p>
 * Definition of weekends may depend on work schedule arrangements and specific industry requirements.
 * For example, some industries may consider Saturday and Sunday as the weekend,
 * while others may schedule rest days between Sunday and Monday.
 * Additionally, some industries may require employees to work overtime or shifts during weekends,
 * resulting in different definitions of weekends for these industries' work arrangements.
 * <p>
 * Cultural and religious beliefs in different ethnic regions may also influence the definition of weekends.
 */
public enum FixaWeekendEnum {
    UNDEFINED(0),
    SATURDAY_AND_SUNDAY(1),
    SUNDAY_AND_MONDAY(2),
    MONDAY_AND_TUESDAY(3),
    TUESDAY_AND_WEDNESDAY(4),
    WEDNESDAY_AND_THURSDAY(5),
    THURSDAY_AND_FRIDAY(6),
    FRIDAY_AND_SATURDAY(7),
    SUNDAY_ONLY(11),
    MONDAY_ONLY(12),
    TUESDAY_ONLY(13),
    WEDNESDAY_ONLY(14),
    THURSDAY_ONLY(15),
    FRIDAY_ONLY(16),
    SATURDAY_ONLY(17);

    private final int code;

    FixaWeekendEnum(int code) {
        this.code = code;
    }

    public static FixaWeekendEnum of(int code) {
        return Stream.of(FixaWeekendEnum.values())
                .filter(p -> p.getCode() == code)
                .findFirst()
                .orElse(SATURDAY_AND_SUNDAY);
    }

    public int getCode() {
        return code;
    }

    public boolean isSingleDayWeekend() {
        return code > 10;
    }

    public boolean isDoubleDayWeekend() {
        return code > 0 && code < 8;
    }
}
