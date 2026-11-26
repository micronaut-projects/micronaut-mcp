# tag::imports[]
from typing import Annotated

from io.modelcontextprotocol.spec import McpError, McpSchema
from jakarta.inject import Singleton
from java.lang import IllegalStateException
from micronaut.context.annotation import Requires
from micronaut.mcp.annotations import Tool, ToolArg
# end::imports[]


@Requires(property="spec.name", value="WeatherToolsTest")
# tag::clazz[]
@Singleton
class WeatherTools:
    TEMPERATURES = {"Madrid": 21, "London": 14}

    @Tool(description="Returns the temperature of a city in degrees Celsius")
    def temperature(self, city: Annotated[str, ToolArg(description="Madrid or London")]) -> McpSchema.CallToolResult:
        temperature = self.TEMPERATURES.get(city)
        if temperature is None:
            return McpSchema.CallToolResult.builder() \
                .addTextContent(f"Unknown city {city}, use Madrid or London") \
                .isError(True) \
                .build()
        return McpSchema.CallToolResult.builder() \
            .addTextContent(str(temperature)) \
            .build()

    @Tool(description="Returns the forecast of a city")
    def forecast(self, city: Annotated[str, ToolArg()]) -> str:
        raise IllegalStateException("The forecast service is not available")

    @Tool(description="Returns the weather alerts of a city")
    def alerts(self, city: Annotated[str, ToolArg()]) -> str:
        raise McpError.builder(McpSchema.ErrorCodes.INTERNAL_ERROR) \
            .message("Weather alerts are disabled on this server") \
            .build()
# end::clazz[]
