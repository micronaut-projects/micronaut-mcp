package example.micronaut.moon.mcp;

import io.micronaut.context.annotation.Property;
import io.micronaut.core.util.StringUtils;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;

@Property(name = "moon.enabled", value = StringUtils.TRUE)
@MicronautTest
class MoonToolsHttpTest {

    @Test
    void moonToolsCallViaHttp(@Client("/") HttpClient httpClient) {
        BlockingHttpClient client = httpClient.toBlocking();
        final String initializeJson = """
            {"method": "initialize", "params": {"protocolVersion": "2025-06-18", "capabilities": {}, "clientInfo": {"name": "claude-ai", "version": "0.1.0"}}, "jsonrpc": "2.0", "id": 0}""";
        assertDoesNotThrow(() -> client.exchange(createRequest(initializeJson)));
        final String toolsCallJson = """
            {
              "method": "tools/call",
              "params": {
                "name": "moon-phase-at-date",
                "arguments": {
                  "date": "1982-10-28"
                },
                "_meta": {
                  "progressToken": 2
                }
              },
              "jsonrpc": "2.0",
              "id": 0
            }
            """;
        String json = assertDoesNotThrow(() -> client.retrieve(createRequest(toolsCallJson)));
        assertFalse(json.contains("error"), json);

    }

    private static HttpRequest<?> createRequest(String json) {
        return HttpRequest.POST("/mcp", json);
    }
}
