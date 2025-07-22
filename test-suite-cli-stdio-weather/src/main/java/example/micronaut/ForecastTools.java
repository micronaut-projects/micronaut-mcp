package example.micronaut;

import example.micronaut.weather.Location;
import example.micronaut.weather.WeatherClient;
import io.micronaut.context.annotation.Factory;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Factory
class ForecastTools {
    public static final String ALERTS_SCHEMA = """
            {
              "type" : "object",
              "id" : "urn:jsonschema:Operation",
              "properties" : {
                "state" : {
                  "type" : "string"
                }
              }
            }
            """;;


    public static final String FORECAST_SCHEMA = """
            {
              "type" : "object",
              "id" : "urn:jsonschema:Operation",
              "properties" : {
                "latitude" : {
                  "type" : "number"
                },
                "longitude" : {
                  "type" : "number"
                }
              }
            }
            """;;

    private final WeatherClient weatherClient;

    ForecastTools(WeatherClient weatherClient) {
        this.weatherClient = weatherClient;
    }

    @Named("getAlerts")
    @Singleton
    McpServerFeatures.SyncToolSpecification getAlertsTools() {
        McpSchema.Tool tool = new McpSchema.Tool("getAlerts",
            "Get weather alerts for a US state.", ALERTS_SCHEMA);
        return new McpServerFeatures.SyncToolSpecification(tool,
                (exchange, arguments) -> {
                    List<McpSchema.Content> contents = new ArrayList<>();
                    boolean isError = false;
                    Object stateObj = arguments.get("state");
                    if (stateObj instanceof String state) {
                        String text = weatherClient.formattedAlerts(state);
                        contents.add(new McpSchema.TextContent(text));
                    } else {
                        isError = true;
                    }
                    return new McpSchema.CallToolResult(contents, isError);
                }
        );
    }

    @Named("getForecast")
    @Singleton
    McpServerFeatures.SyncToolSpecification getForecast() {
        McpSchema.Tool tool = new McpSchema.Tool("getForecast",
            "Get weather forecast for a location.", FORECAST_SCHEMA);
        return new McpServerFeatures.SyncToolSpecification(tool,
            (exchange, arguments) -> {
                Optional<Location> location = Location.of(arguments);
                if (location.isEmpty()) {
                    return new McpSchema.CallToolResult(Collections.emptyList(), true);
                }
                String text = weatherClient.formattedForecast(location.get());
                return new McpSchema.CallToolResult(List.of(new McpSchema.TextContent(text)), false);
            }
        );
    }
}
