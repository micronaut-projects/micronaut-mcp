package io.modelcontextprotocol.server.http.tck.async;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.mcp.annotations.Tool;
import jakarta.inject.Singleton;

@Singleton
public class Tools {
    @Tool(name = "get_weather", title = "Weather Information Provider", description = "Get current weather information for a location")
    String getWeather(@NonNull GetWeatherInput input) {
        return "Sunny";
    }
}
