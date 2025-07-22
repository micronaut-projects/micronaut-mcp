package example.micronaut.weather;

import io.micronaut.core.util.StringUtils;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

@MicronautTest(startApplication = false)
class WeatherClientTest {
    private final double newYorkLatitude = 40.712776;
    private final double newYorkLongitude = -74.005974;

    @Test
    void weatherAlerts(WeatherClient weatherClient) {
        assertDoesNotThrow(() -> weatherClient.getAlerts("NY"));
    }

    @Test
    void weatherAlertsFormatted(WeatherClient weatherClient) {
        String alerts = assertDoesNotThrow(() -> weatherClient.formattedAlerts("NY"));
        assertTrue(StringUtils.isNotEmpty(alerts));
    }

    @Test
    void weatherPoints(WeatherClient weatherClient) {
        assertDoesNotThrow(() -> weatherClient.getPoints(newYorkLatitude, newYorkLongitude));
    }

    @Test
    void weatherForecast(WeatherClient weatherClient) {
        Optional<Forecast> forecast = assertDoesNotThrow(() -> weatherClient.getForecast(newYorkLatitude, newYorkLongitude));
        assertTrue(forecast.isPresent());
    }
}
