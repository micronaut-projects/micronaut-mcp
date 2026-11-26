package io.micronaut.mcp.server;

import io.micronaut.context.ApplicationContext;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.HttpClient;
import io.micronaut.runtime.server.EmbeddedServer;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpStatelessAsyncServer;
import io.modelcontextprotocol.server.McpStatelessSyncServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.spec.McpSchema;
import org.json.JSONException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Each of the four server factories sends the server description and the instructions.
 */
class ServerDescriptionFactoriesTest {

    private static final String INSTRUCTIONS = "Ask for a city first.";

    private static final McpSchema.Implementation SERVER_INFO = new McpSchema.Implementation("weather", "Weather", "1.0.0",
        "Forecasts by city",
        List.of(new McpSchema.Icon("https://example.com/weather-light.svg", "image/svg+xml", List.of("any"), "light"),
            new McpSchema.Icon("https://example.com/weather-dark.png", "image/png", List.of("48x48", "96x96"), "dark")),
        "https://example.com");

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void httpServersSendTheServerDescription(boolean reactive) throws JSONException {
        try (EmbeddedServer server = ApplicationContext.run(EmbeddedServer.class, properties("HTTP", reactive));
             HttpClient client = HttpClient.create(server.getURL())) {
            assertTrue(server.getApplicationContext().containsBean(reactive ? McpStatelessAsyncServer.class : McpStatelessSyncServer.class));
            String response = client.toBlocking().retrieve(HttpRequest.POST("/mcp", """
                {"jsonrpc": "2.0", "id": 1, "method": "initialize",
                 "params": {"protocolVersion": "2025-06-18", "capabilities": {}, "clientInfo": {"name": "test", "version": "1"}}}"""));
            JSONAssert.assertEquals("""
                {"result": {
                  "serverInfo": {"name": "weather", "title": "Weather", "version": "1.0.0", "description": "Forecasts by city",
                    "icons": [{"src": "https://example.com/weather-light.svg", "mimeType": "image/svg+xml", "sizes": ["any"], "theme": "light"},
                              {"src": "https://example.com/weather-dark.png", "mimeType": "image/png", "sizes": ["48x48", "96x96"], "theme": "dark"}],
                    "websiteUrl": "https://example.com"},
                  "instructions": "Ask for a city first."}}""", response, JSONCompareMode.STRICT_ORDER);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void stdioServersSendTheServerDescription(boolean reactive) throws ReflectiveOperationException {
        try (ApplicationContext context = ApplicationContext.run(properties("STDIO", reactive))) {
            McpAsyncServer server = reactive
                ? context.getBean(McpAsyncServer.class)
                : context.getBean(McpSyncServer.class).getAsyncServer();
            assertEquals(SERVER_INFO, server.getServerInfo());
            // the SDK exposes no getter for the instructions it sends in the response to initialize
            Field instructions = McpAsyncServer.class.getDeclaredField("instructions");
            instructions.setAccessible(true);
            assertEquals(INSTRUCTIONS, instructions.get(server));
        }
    }

    private static Map<String, Object> properties(String transport, boolean reactive) {
        return Map.ofEntries(
            Map.entry("micronaut.mcp.server.transport", transport),
            Map.entry("micronaut.mcp.server.reactive", reactive),
            Map.entry("micronaut.mcp.server.info.name", "weather"),
            Map.entry("micronaut.mcp.server.info.version", "1.0.0"),
            Map.entry("micronaut.mcp.server.info.title", "Weather"),
            Map.entry("micronaut.mcp.server.info.description", "Forecasts by city"),
            Map.entry("micronaut.mcp.server.info.website-url", "https://example.com"),
            Map.entry("micronaut.mcp.server.info.icons[0].src", "https://example.com/weather-light.svg"),
            Map.entry("micronaut.mcp.server.info.icons[0].mime-type", "image/svg+xml"),
            Map.entry("micronaut.mcp.server.info.icons[0].sizes", "any"),
            Map.entry("micronaut.mcp.server.info.icons[0].theme", "light"),
            Map.entry("micronaut.mcp.server.info.icons[1].src", "https://example.com/weather-dark.png"),
            Map.entry("micronaut.mcp.server.info.icons[1].mime-type", "image/png"),
            Map.entry("micronaut.mcp.server.info.icons[1].sizes", List.of("48x48", "96x96")),
            Map.entry("micronaut.mcp.server.info.icons[1].theme", "dark"),
            Map.entry("micronaut.mcp.server.info.instructions", INSTRUCTIONS));
    }
}
