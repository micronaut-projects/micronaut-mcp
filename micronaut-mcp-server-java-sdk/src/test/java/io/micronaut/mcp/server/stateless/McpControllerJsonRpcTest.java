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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.skyscreamer.jsonassert.JSONAssert;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Property(name = "micronaut.mcp.server.info.name", value = "mcp-server")
@Property(name = "micronaut.mcp.server.info.version", value = "0.0.1")
@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "spec.name", value = "McpControllerJsonRpcTest")
@MicronautTest
class McpControllerJsonRpcTest {

    private static final String INVALID_REQUEST = """
        {
          "jsonrpc": "2.0",
          "id": null,
          "error": {"code": -32600, "message": "The server accepts either JSON-RPC requests or notifications"}
        }""";

    @Test
    void aMessageWithoutJsonRpcVersionIsAnInvalidRequest(@Client("/") HttpClient httpClient) throws JSONException {
        assertInvalidRequest(httpClient, """
            {"method": "tools/list", "id": 1}""");
    }

    @Test
    void aBodyThatIsNotAnObjectIsAnInvalidRequest(@Client("/") HttpClient httpClient) throws JSONException {
        assertInvalidRequest(httpClient, """
            [{"jsonrpc": "2.0", "method": "tools/list", "id": 1}]""");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        """
        {"jsonrpc": 2, "method": "tools/list", "id": 1}""",
        """
        {"jsonrpc": "1.0", "method": "tools/list", "id": 1}""",
        """
        {"jsonrpc": "2.0", "method": 1, "id": 1}""",
        """
        {"jsonrpc": "2.0", "id": 1}""",
        """
        {"jsonrpc": "2.0", "id": true, "method": "ping"}""",
        """
        {"jsonrpc": "2.0", "id": null, "method": "ping"}""",
        """
        {"jsonrpc": "2.0", "id": 1.0, "method": "ping"}""",
        """
        {"jsonrpc": "2.0", "id": 1.5, "method": "ping"}""",
        """
        {"jsonrpc": "2.0", "id": 12345678901234567890, "method": "ping"}"""
    })
    void aMessageWithAnInvalidVersionMethodOrIdIsAnInvalidRequest(String body, @Client("/") HttpClient httpClient) throws JSONException {
        assertInvalidRequest(httpClient, body);
    }

    @Test
    void longIdsAreAccepted(@Client("/") HttpClient httpClient) throws JSONException {
        BlockingHttpClient client = httpClient.toBlocking();
        HttpResponse<String> response = client.exchange(HttpRequest.POST("/mcp", """
            {"jsonrpc": "2.0", "id": 12345678901, "method": "ping"}"""), String.class);
        assertEquals(HttpStatus.OK, response.getStatus());
        JSONAssert.assertEquals("""
            {"jsonrpc": "2.0", "id": 12345678901, "result": {}}""", response.body(), true);
    }

    @Test
    void paramsTheSdkCannotConvertAreAJsonRpcError(@Client("/") HttpClient httpClient) throws JSONException {
        BlockingHttpClient client = httpClient.toBlocking();
        HttpRequest<String> request = HttpRequest.POST("/mcp", """
            {"jsonrpc": "2.0", "id": 3, "method": "tools/call", "params": "x"}""");
        HttpClientResponseException ex = assertThrows(HttpClientResponseException.class, () -> client.exchange(request, String.class));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatus());
        JSONAssert.assertEquals("""
            {"jsonrpc": "2.0", "id": 3, "error": {"code": -32603}}""", ex.getResponse().getBody(String.class).orElseThrow(), false);
    }

    private static void assertInvalidRequest(HttpClient httpClient, String body) throws JSONException {
        BlockingHttpClient client = httpClient.toBlocking();
        HttpRequest<String> request = HttpRequest.POST("/mcp", body);
        HttpClientResponseException ex = assertThrows(HttpClientResponseException.class, () -> client.exchange(request, String.class));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        JSONAssert.assertEquals(INVALID_REQUEST, ex.getResponse().getBody(String.class).orElseThrow(), true);
    }

    @Test
    void nullParamsAreAccepted(@Client("/") HttpClient httpClient) throws JSONException {
        BlockingHttpClient client = httpClient.toBlocking();
        HttpResponse<String> response = client.exchange(HttpRequest.POST("/mcp", """
            {"jsonrpc": "2.0", "id": 7, "method": "ping", "params": null}"""), String.class);
        assertEquals(HttpStatus.OK, response.getStatus());
        JSONAssert.assertEquals("""
            {"jsonrpc": "2.0", "id": 7, "result": {}}""", response.body(), true);
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
