package example.micronaut.mcp;

import io.micronaut.context.ApplicationContext;
import io.micronaut.runtime.server.EmbeddedServer;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ToolsTest {
    // Latitude 37.3349° N and Longitude -122.0090° W (Apple Park, Cupertino) in microdegrees.
    public static final double LATITUDE = 37.3349;
    public static final double LONGITUDE = -122.0090;
    public static final String MICRONAUT_HTTP_SERVICES_WEATHER_URL = "micronaut.http.services.weather.url";

    @Test
    void getWeatherForecastByLocation() {
        try (EmbeddedServer mockServer = ApplicationContext.run(EmbeddedServer.class)) {
            Map<String, Object> config = Map.of(MICRONAUT_HTTP_SERVICES_WEATHER_URL, mockServer.getURL().toString());
            try (ApplicationContext ctx = ApplicationContext.run(config)) {
                Tools tools = ctx.getBean(Tools.class);
                assertDoesNotThrow(() -> tools.getWeatherForecastByLocation(LATITUDE, LONGITUDE));
            }
        }
    }

    @Test
    void getAlerts() {
        try (EmbeddedServer mockServer = ApplicationContext.run(EmbeddedServer.class)) {
            Map<String, Object> config = Map.of(MICRONAUT_HTTP_SERVICES_WEATHER_URL, mockServer.getURL().toString());
            try (ApplicationContext ctx = ApplicationContext.run(config)) {
                Tools tools = ctx.getBean(Tools.class);
                assertDoesNotThrow(() -> tools.getAlerts(new GetAlertInput("CA")));
            }
        }

    }

}