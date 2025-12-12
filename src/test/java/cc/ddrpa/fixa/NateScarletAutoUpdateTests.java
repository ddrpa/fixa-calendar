package cc.ddrpa.fixa;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * You can use @Scheduled annotation in Spring Boot project to schedule tasks.
 * <p>
 * Use @Scheduled(cron = "0 0 0 25 12 ?") to update holiday data every Christmas Day at midnight.
 */
class NateScarletAutoUpdateTests {
    private static final FixaCalendar calendar = new FixaCalendar(FixaWeekendEnum.SATURDAY_AND_SUNDAY, LocalDate.of(2023, 12, 25), Duration.ofDays(400));

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

    /**
     * See <a href="https://github.com/NateScarlet/holiday-cn">NateScarlet/holiday-cn - GitHub</a> for more information.
     */
    @Test
    void autoUpdateTest() throws URISyntaxException, IOException, InterruptedException {
        var request = HttpRequest.newBuilder()
                .uri(new URI(String.format("https://fastly.jsdelivr.net/gh/NateScarlet/holiday-cn@master/%s.json", 2024)))
                .GET()
                .timeout(Duration.of(3, SECONDS))
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        var days = new JsonParser().parse(response.body()).getAsJsonObject().getAsJsonArray("days");
        for (var day : days) {
            JsonObject dayObject = day.getAsJsonObject();
            var date = LocalDate.parse(dayObject.get("date").getAsString());
            if (dayObject.get("isOffDay").getAsBoolean()) {
                calendar.addHoliday(date);
            } else {
                calendar.addFlexibleWorkday(date);
            }
        }

        for (var holiday : DATA_HOLIDAYS) {
            assertTrue(calendar.isHoliday(holiday));
            assertTrue(calendar.isDayOff(holiday));
        }
        for (var workday : DATA_FLEXIBLE_WORKDAYS) {
            assertTrue(calendar.isFlexibleWorkday(workday));
            assertFalse(calendar.isDayOff(workday));
        }
    }
}