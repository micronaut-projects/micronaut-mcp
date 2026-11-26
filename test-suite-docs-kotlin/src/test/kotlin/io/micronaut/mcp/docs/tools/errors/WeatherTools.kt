package io.micronaut.mcp.docs.tools.errors
/*
//tag::fakepackage[]
package example.micronaut

//end::fakepackage[]
 */
//tag::imports[]
import io.micronaut.context.annotation.Requires
import io.micronaut.mcp.annotations.Tool
import io.micronaut.mcp.annotations.ToolArg
import io.modelcontextprotocol.spec.McpError
import io.modelcontextprotocol.spec.McpSchema
import jakarta.inject.Singleton

//end::imports[]

@Requires(property = "spec.name", value = "WeatherToolsTest")
//tag::clazz[]
@Singleton
class WeatherTools {
    private val temperatures = mapOf("Madrid" to 21, "London" to 14)

    @Tool(description = "Returns the temperature of a city in degrees Celsius")
    fun temperature(@ToolArg(description = "Madrid or London") city: String): McpSchema.CallToolResult {
        val temperature = temperatures[city]
            ?: return McpSchema.CallToolResult.builder()
                .addTextContent("Unknown city $city, use Madrid or London")
                .isError(true)
                .build()
        return McpSchema.CallToolResult.builder()
            .addTextContent(temperature.toString())
            .build()
    }

    @Tool(description = "Returns the forecast of a city")
    fun forecast(@ToolArg city: String): String {
        throw IllegalStateException("The forecast service is not available")
    }

    @Tool(description = "Returns the weather alerts of a city")
    fun alerts(@ToolArg city: String): String {
        throw McpError.builder(McpSchema.ErrorCodes.INTERNAL_ERROR)
            .message("Weather alerts are disabled on this server")
            .build()
    }
}
//end::clazz[]
