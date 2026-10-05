package io.micronaut.mcp.server.stateless;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.mcp.annotations.Tool;
import io.micronaut.mcp.server.context.MicronautMcpTransportContext;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Singleton;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Property(name = "micronaut.mcp.server.info.name", value = "mcp-server")
@Property(name = "micronaut.mcp.server.info.version", value = "0.0.1")
@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "spec.name", value = "McpControllerJsonRpcTest")
@MicronautTest
class McpControllerJsonRpcTest {

    @Test
    void aMessageWithoutJsonRpcVersionIsABadRequest(@Client("/") HttpClient httpClient) {
        BlockingHttpClient client = httpClient.toBlocking();
        HttpClientResponseException ex = assertThrows(HttpClientResponseException.class, () -> client.exchange(HttpRequest.POST("/mcp", """
            {"method": "tools/list", "id": 1}"""), String.class));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals(-32600, ex.getResponse().getBody(java.util.Map.class).map(m -> ((Number) ((java.util.Map<?, ?>) m.get("error")).get("code")).intValue()).orElse(0));
    }

    @Test
    void aBodyThatIsNotAnObjectIsABadRequest(@Client("/") HttpClient httpClient) {
        BlockingHttpClient client = httpClient.toBlocking();
        HttpClientResponseException ex = assertThrows(HttpClientResponseException.class, () -> client.exchange(HttpRequest.POST("/mcp", """
            [{"jsonrpc": "2.0", "method": "tools/list", "id": 1}]"""), String.class));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void notificationsAreAccepted(@Client("/") HttpClient httpClient) {
        BlockingHttpClient client = httpClient.toBlocking();
        HttpResponse<String> response = client.exchange(HttpRequest.POST("/mcp", """
            {"jsonrpc": "2.0", "method": "notifications/initialized"}"""), String.class);
        assertEquals(HttpStatus.ACCEPTED, response.getStatus());
    }

    @Test
    void stringIdsAndTransportContextValuesAreKept(@Client("/") HttpClient httpClient) throws JSONException {
        BlockingHttpClient client = httpClient.toBlocking();
        HttpResponse<String> response = client.exchange(HttpRequest.POST("/mcp", """
            {
              "jsonrpc": "2.0",
              "id": "call-1",
              "method": "tools/call",
              "params": {"name": "context", "arguments": {}}
            }""").header("MCP-Protocol-Version", "2025-06-18"), String.class);
        assertEquals(HttpStatus.OK, response.getStatus());
        JSONAssert.assertEquals("""
            {
              "jsonrpc": "2.0",
              "id": "call-1",
              "result": {
                "content": [{"type": "text", "text": "2025-06-18 true null"}],
                "isError": false
              }
            }""", response.body(), true);
    }

    @Requires(property = "spec.name", value = "McpControllerJsonRpcTest")
    @Singleton
    static class Tools {
        @Tool
        String context(MicronautMcpTransportContext ctx) {
            return ctx.protocolVersion() + " " + (ctx.host() != null) + " " + ctx.sessionId();
        }
    }
}
