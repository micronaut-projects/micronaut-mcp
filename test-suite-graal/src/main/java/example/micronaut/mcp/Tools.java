package example.micronaut.mcp;

import example.micronaut.weather.WeatherClient;
import io.micronaut.mcp.annotations.Tool;
import jakarta.inject.Singleton;

@Singleton
class Tools {

    private final WeatherClient weatherClient;

    Tools(WeatherClient weatherClient) {
        this.weatherClient = weatherClient;
    }

    @Tool(description = "Get weather forecast for a specific latitude/longitude coordinates")
    String getWeatherForecastByLocation(double latitude, double longitude) {
        return weatherClient.formattedForecast(latitude, longitude);
    }

    @Tool(description = "Get weather alerts for a US state")
    String getAlerts(GetAlertInput input) {
        return weatherClient.formattedAlerts(input.state());
    }
}
