package example.micronaut.moon.mcp;

import io.micronaut.context.annotation.Property;
import io.micronaut.core.util.StringUtils;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@Property(name = "moon.enabled", value = StringUtils.TRUE)
@MicronautTest
class MoonToolsHttpTest {
    @Test
    void invalidParamsCannotDeserialize(@Client("/") HttpClient httpClient) throws JSONException {
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
        // An argument that does not match the input schema is a tool execution error the model can correct
        String response = assertDoesNotThrow(() -> client.retrieve(createRequest(toolsCallJson)));
        JSONAssert.assertEquals("""
            {"jsonrpc":"2.0","id":0,"result":{"content":[{"type":"text","text":"Tool (moon-phase-at-date) input validation failed: /date: does not match the date pattern must be a valid RFC 3339 full-date"}],"isError":true}}
            """, response, true);
    }

    private static HttpRequest<?> createRequest(String json) {
        return HttpRequest.POST("/mcp", json);
    }
}
