package example.micronaut.moon.mcp;

import io.micronaut.context.annotation.Property;
import io.micronaut.core.util.StringUtils;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
        HttpClientResponseException ex = assertThrows(HttpClientResponseException.class, () ->
            client.retrieve(createRequest(toolsCallJson), McpSchema.JSONRPCResponse.class));
        Optional<McpSchema.JSONRPCResponse> jsonrpcResponseOptional = ex.getResponse().getBody(McpSchema.JSONRPCResponse.class);
        assertTrue(jsonrpcResponseOptional.isPresent());
        McpSchema.JSONRPCResponse json = jsonrpcResponseOptional.get();
        assertNotNull(json.error());
        assertEquals(-32602, json.error().code());
        assertEquals("Required argument [MoonPhaseRequest moonPhaseRequest] not specified", json.error().message());
    }

    private static HttpRequest<?> createRequest(String json) {
        return HttpRequest.POST("/mcp", json);
    }
}
