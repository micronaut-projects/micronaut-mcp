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
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.skyscreamer.jsonassert.JSONAssert;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Property(name = "micronaut.mcp.server.info.name", value = "mcp-server")
@Property(name = "micronaut.mcp.server.info.version", value = "0.0.1")
@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "spec.name", value = "McpControllerJsonRpcTest")
@MicronautTest
class McpControllerJsonRpcTest {

    static Stream<Arguments> invalidRequests() {
        String jsonrpc = "Invalid Request: \\\"jsonrpc\\\" must be \\\"2.0\\\"";
        String method = "Invalid Request: \\\"method\\\" must be a string";
        String id = "Invalid Request: \\\"id\\\" must be a string or an integer";
        String params = "Invalid Request: \\\"params\\\" must be an object or an array";
        return Stream.of(
            Arguments.of("""
                [{"jsonrpc": "2.0", "method": "tools/list", "id": 1}]""",
                "Invalid Request: batches are not supported, send one JSON-RPC message per request"),
            Arguments.of("\"ping\"", "Invalid Request: the body must be a JSON object"),
            Arguments.of("""
                {"method": "tools/list", "id": 1}""", jsonrpc),
            Arguments.of("""
                {"jsonrpc": 2, "method": "tools/list", "id": 1}""", jsonrpc),
            Arguments.of("""
                {"jsonrpc": "1.0", "method": "tools/list", "id": 1}""", jsonrpc),
            Arguments.of("""
                {"jsonrpc": "2.0", "method": 1, "id": 1}""", method),
            Arguments.of("""
                {"jsonrpc": "2.0", "id": 1}""", method),
            Arguments.of("""
                {"jsonrpc": "2.0", "id": true, "method": "ping"}""", id),
            Arguments.of("""
                {"jsonrpc": "2.0", "id": null, "method": "ping"}""", id),
            Arguments.of("""
                {"jsonrpc": "2.0", "id": 1.0, "method": "ping"}""", id),
            Arguments.of("""
                {"jsonrpc": "2.0", "id": 1.5, "method": "ping"}""", id),
            Arguments.of("""
                {"jsonrpc": "2.0", "id": 12345678901234567890, "method": "ping"}""", id),
            Arguments.of("""
                {"jsonrpc": "2.0", "id": 3, "method": "tools/call", "params": "x"}""", params),
            Arguments.of("""
                {"jsonrpc": "2.0", "method": "notifications/initialized", "params": 1}""", params));
    }

    @ParameterizedTest
    @MethodSource("invalidRequests")
    void aMessageThatIsNotAValidRequestOrNotificationIsAnInvalidRequest(String body, String message,
                                                                         @Client("/") HttpClient httpClient) throws JSONException {
        BlockingHttpClient client = httpClient.toBlocking();
        HttpRequest<String> request = HttpRequest.POST("/mcp", body);
        HttpClientResponseException ex = assertThrows(HttpClientResponseException.class, () -> client.exchange(request, String.class));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        JSONAssert.assertEquals("""
            {"jsonrpc": "2.0", "id": null, "error": {"code": -32600, "message": "%s"}}""".formatted(message),
            ex.getResponse().getBody(String.class).orElseThrow(), true);
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
    void paramsTheSdkCannotConvertAreAnInternalErrorWithoutItsMessage(@Client("/") HttpClient httpClient) throws JSONException {
        BlockingHttpClient client = httpClient.toBlocking();
        HttpRequest<String> request = HttpRequest.POST("/mcp", """
            {"jsonrpc": "2.0", "id": 3, "method": "tools/call", "params": [1, 2]}""");
        HttpClientResponseException ex = assertThrows(HttpClientResponseException.class, () -> client.exchange(request, String.class));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatus());
        JSONAssert.assertEquals("""
            {"jsonrpc": "2.0", "id": 3, "error": {"code": -32603, "message": "Internal error"}}""",
            ex.getResponse().getBody(String.class).orElseThrow(), true);
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
