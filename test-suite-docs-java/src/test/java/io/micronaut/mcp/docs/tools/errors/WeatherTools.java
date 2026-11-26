package io.micronaut.mcp.docs.tools.errors;
/*
//tag::fakepackage[]
package example.micronaut;

//end::fakepackage[]
 */
//tag::imports[]
import io.micronaut.context.annotation.Requires;
import io.micronaut.mcp.annotations.Tool;
import io.micronaut.mcp.annotations.ToolArg;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Singleton;

import java.util.Map;

//end::imports[]

@Requires(property = "spec.name", value = "WeatherToolsTest")
//tag::clazz[]
@Singleton
class WeatherTools {
    private static final Map<String, Integer> TEMPERATURES = Map.of("Madrid", 21, "London", 14);

    @Tool(description = "Returns the temperature of a city in degrees Celsius")
    McpSchema.CallToolResult temperature(@ToolArg(description = "Madrid or London") String city) {
        Integer temperature = TEMPERATURES.get(city);
        if (temperature == null) {
            return McpSchema.CallToolResult.builder()
                .addTextContent("Unknown city " + city + ", use Madrid or London")
                .isError(true)
                .build();
        }
        return McpSchema.CallToolResult.builder()
            .addTextContent(String.valueOf(temperature))
            .build();
    }

    @Tool(description = "Returns the forecast of a city")
    String forecast(@ToolArg String city) {
        throw new IllegalStateException("The forecast service is not available");
    }

    @Tool(description = "Returns the weather alerts of a city")
    String alerts(@ToolArg String city) {
        throw McpError.builder(McpSchema.ErrorCodes.INTERNAL_ERROR)
            .message("Weather alerts are disabled on this server")
            .build();
    }
}
//end::clazz[]
