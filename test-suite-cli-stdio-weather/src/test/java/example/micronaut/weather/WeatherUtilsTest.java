package example.micronaut.weather;

import io.micronaut.core.type.Argument;
import io.micronaut.json.JsonMapper;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest(startApplication = false)
class WeatherUtilsTest {

    @Test
    void parseForecastUrl(JsonMapper jsonMapper) throws IOException {
        Map<String, Object> points = jsonMapper.readValue(WeatherClientServerMockTest.POINTS_EXAMPLE, Argument.mapOf(String.class, Object.class));
        Optional<String> forecastUrl = WeatherUtils.getForecastUrl(points);
        assertTrue(forecastUrl.isPresent());
        assertEquals("https://api.weather.gov/gridpoints/OKX/33,35/forecast", forecastUrl.get());
    }

    @Test
    void parseGrid() {
        Grid expected = new Grid("OKX", "33", "35");
        Optional<Grid> grid = WeatherUtils.parseGrid("https://api.weather.gov/gridpoints/OKX/33,35/forecast");
        assertTrue(grid.isPresent());
        assertEquals(expected, grid.get());
    }
}
