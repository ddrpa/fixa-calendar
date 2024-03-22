package cc.ddrpa.fixa;

import java.time.LocalDate;
import java.util.List;

public class TestCases {
    public static final List<LocalDate> DATA_HOLIDAYS = List.of(
            LocalDate.of(2024, 4, 22),
            LocalDate.of(2024, 4, 23),
            LocalDate.of(2024, 4, 24),
            LocalDate.of(2024, 4, 25),
            LocalDate.of(2024, 5, 1),
            LocalDate.of(2024, 5, 2));
    public static final List<LocalDate> DATA_FLEXIBLE_WORKDAYS = List.of(
            LocalDate.of(2024, 3, 30),
            LocalDate.of(2024, 3, 31),
            LocalDate.of(2024, 4, 19),
            LocalDate.of(2024, 4, 20),
            LocalDate.of(2024, 5, 4));
}