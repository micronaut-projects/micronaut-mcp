package example.micronaut;

import io.micronaut.context.exceptions.ConfigurationException;
import io.micronaut.core.io.ResourceLoader;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Produces;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static example.micronaut.ResourceLoaderUtils.readResource;

@Controller
class WeatherMockController {
    private final String alerts;
    private final String points;
    private final String forecast;

    WeatherMockController(ResourceLoader resourceLoader) {
        try {
            this.points = readResource(resourceLoader, "classpath:points.json")
                .orElseThrow(() -> new ConfigurationException("Could not read points.json"));
            this.forecast = readResource(resourceLoader, "classpath:forecast.json")
                    .orElseThrow(() -> new ConfigurationException("Could not read forecast.json"));
            this.alerts = readResource(resourceLoader, "classpath:alerts.json")
                    .orElseThrow(() -> new ConfigurationException("Could not read alerts.json"));
        } catch (IOException e) {
            throw new ConfigurationException("Could not read points.json");
        }
    }

    @Produces({"application/geo+json", "application/json"})
    @Get("/points/37.3349,-122.009")
    String points() {
        return points;
    }

    @Produces({"application/geo+json", "application/json"})
    @Get("/gridpoints/MTR/95,83/forecast")
    String forecast() {
        return forecast;
    }

    @Produces({"application/geo+json", "application/json"})
    @Get("/alerts/active/area/CA")
    String alerts() {
        return alerts;
    }



}
