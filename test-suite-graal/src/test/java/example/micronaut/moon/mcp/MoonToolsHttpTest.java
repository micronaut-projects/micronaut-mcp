package example.micronaut.moon.mcp;

import io.micronaut.context.annotation.Property;
import io.micronaut.core.util.StringUtils;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Property(name = "moon.enabled", value = StringUtils.TRUE)
@MicronautTest
class MoonToolsHttpTest {
    @Test
    void invalidParamsCannotDeserialize(@Client("/") HttpClient httpClient) {
        BlockingHttpClient client = httpClient.toBlocking();
        final String toolsCallJson = """
            {
              "method": "tools/call",
              "params": {
                "name": "moon-phase-at-date",
                "arguments": {
                  "date": "1982-30-28"
                },
                "_meta": {
                  "progressToken": 2
                }
              },
              "jsonrpc": "2.0",
              "id": 0
            }
            """;
        McpSchema.JSONRPCResponse json = assertDoesNotThrow(() -> client.retrieve(createRequest(toolsCallJson), McpSchema.JSONRPCResponse.class));
        assertNotNull(json.error());
        assertEquals(-32602, json.error().code());
        assertEquals("Invalid value for 'date': '1982-30-28'", json.error().message());
    }

    @Test
    void invalidParamsConstraintViolationException(@Client("/") HttpClient httpClient) {
        BlockingHttpClient client = httpClient.toBlocking();
        final String toolsCallJson = """
            {
              "method": "tools/call",
              "params": {
                "name": "moon-phase-at-date",
                "arguments": {
                  "date": "2099-10-28"
                },
                "_meta": {
                  "progressToken": 2
                }
              },
              "jsonrpc": "2.0",
              "id": 0
            }
            """;
        McpSchema.JSONRPCResponse json = assertDoesNotThrow(() -> client.retrieve(createRequest(toolsCallJson), McpSchema.JSONRPCResponse.class));
        assertNotNull(json.error());
        assertEquals(-32602, json.error().code());
        assertEquals("date: must be a date in the past or in the present", json.error().message());
    }

    @Test
    void moonToolsCallViaHttp(@Client("/") HttpClient httpClient) {
        BlockingHttpClient client = httpClient.toBlocking();
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
