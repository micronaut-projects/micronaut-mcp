package io.micronaut.mcp.server.stateless;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.json.tree.JsonNode;
import io.micronaut.mcp.annotations.Tool;
import io.micronaut.mcp.annotations.ToolArg;
import io.micronaut.mcp.server.context.MicronautMcpTransportContext;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.server.McpStatelessServerHandler;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Singleton;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.skyscreamer.jsonassert.JSONAssert;
import reactor.core.publisher.Mono;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
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
            Arguments.of("1", "Invalid Request: the body must be a JSON object"),
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
                {"jsonrpc": "2.0", "id": 9223372036854775808, "method": "ping"}""", id),
            Arguments.of("""
                {"jsonrpc": "2.0", "id": true, "method": "notifications/initialized"}""", id),
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

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "12345678901", "9223372036854775807", "\"\"", "\"call-1\""})
    void stringAndIntegerIdsAreEchoed(String id, @Client("/") HttpClient httpClient) throws JSONException {
        BlockingHttpClient client = httpClient.toBlocking();
        HttpResponse<String> response = client.exchange(HttpRequest.POST("/mcp", """
            {"jsonrpc": "2.0", "id": %s, "method": "ping"}""".formatted(id)), String.class);
        assertEquals(HttpStatus.OK, response.getStatus());
        JSONAssert.assertEquals("""
            {"jsonrpc": "2.0", "id": %s, "result": {}}""".formatted(id), response.body(), true);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "[]", "null"})
    void objectArrayAndNullParamsAreAccepted(String params, @Client("/") HttpClient httpClient) throws JSONException {
        BlockingHttpClient client = httpClient.toBlocking();
        HttpResponse<String> response = client.exchange(HttpRequest.POST("/mcp", """
            {"jsonrpc": "2.0", "id": 7, "method": "ping", "params": %s}""".formatted(params)), String.class);
        assertEquals(HttpStatus.OK, response.getStatus());
        JSONAssert.assertEquals("""
            {"jsonrpc": "2.0", "id": 7, "result": {}}""", response.body(), true);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "null"})
    void anEmptyOrNullBodyIsABadRequest(String body, @Client("/") HttpClient httpClient) {
        BlockingHttpClient client = httpClient.toBlocking();
        HttpRequest<String> request = HttpRequest.POST("/mcp", body).contentType(MediaType.APPLICATION_JSON_TYPE);
        HttpClientResponseException ex = assertThrows(HttpClientResponseException.class, () -> client.exchange(request, String.class));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void theParametersReachTheSdkAsTheParsedTree(@Client("/") HttpClient httpClient, ParamsRecorder recorder) throws JSONException {
        BlockingHttpClient client = httpClient.toBlocking();
        HttpResponse<String> response = client.exchange(HttpRequest.POST("/mcp", """
            {"jsonrpc": "2.0", "id": 8, "method": "tools/call", "params": {"name": "echo", "arguments": {"text": "hello"}}}"""),
            String.class);
        assertEquals(HttpStatus.OK, response.getStatus());
        JSONAssert.assertEquals("""
            {"jsonrpc": "2.0", "id": 8, "result": {"content": [{"type": "text", "text": "hello"}], "isError": false}}""",
            response.body(), true);
        assertInstanceOf(JsonNode.class, recorder.lastParams);
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
    void notificationsAreAccepted(@Client("/") HttpClient httpClient) {
        BlockingHttpClient client = httpClient.toBlocking();
        HttpResponse<String> response = client.exchange(HttpRequest.POST("/mcp", """
            {"jsonrpc": "2.0", "method": "notifications/initialized"}"""), String.class);
        assertEquals(HttpStatus.ACCEPTED, response.getStatus());
    }

    @Test
    void theTransportContextReadsTheRequestHeaders(@Client("/") HttpClient httpClient) throws JSONException {
        BlockingHttpClient client = httpClient.toBlocking();
        HttpResponse<String> response = client.exchange(HttpRequest.POST("/mcp", """
            {
              "jsonrpc": "2.0",
              "id": "call-1",
              "method": "tools/call",
              "params": {"name": "context", "arguments": {}}
            }""")
            .header("MCP-Protocol-Version", "2025-06-18")
            .header("Mcp-Session-Id", "session-1")
            .header("Last-Event-ID", "event-1"), String.class);
        assertEquals(HttpStatus.OK, response.getStatus());
        JSONAssert.assertEquals("""
            {
              "jsonrpc": "2.0",
              "id": "call-1",
              "result": {
                "content": [{"type": "text", "text": "2025-06-18 true session-1 event-1"}],
                "isError": false
              }
            }""", response.body(), true);
    }

    @Requires(property = "spec.name", value = "McpControllerJsonRpcTest")
    @Singleton
    static class Tools {
        @Tool
        String context(MicronautMcpTransportContext ctx) {
            return ctx.protocolVersion() + " " + (ctx.host() != null) + " " + ctx.sessionId() + " " + ctx.lastEventId();
        }

        @Tool
        String echo(@ToolArg(name = "text") String text) {
            return text;
        }
    }

    /**
     * Records the parameters of the requests the controller hands to the SDK handler.
     */
    @Requires(property = "spec.name", value = "McpControllerJsonRpcTest")
    @Singleton
    static class ParamsRecorder implements BeanCreatedEventListener<McpStatelessServerHandler> {
        private volatile Object lastParams;

        @Override
        public McpStatelessServerHandler onCreated(BeanCreatedEvent<McpStatelessServerHandler> event) {
            McpStatelessServerHandler handler = event.getBean();
            return new McpStatelessServerHandler() {
                @Override
                public Mono<McpSchema.JSONRPCResponse> handleRequest(McpTransportContext transportContext, McpSchema.JSONRPCRequest request) {
                    lastParams = request.params();
                    return handler.handleRequest(transportContext, request);
                }

                @Override
                public Mono<Void> handleNotification(McpTransportContext transportContext, McpSchema.JSONRPCNotification notification) {
                    return handler.handleNotification(transportContext, notification);
                }
            };
        }
    }
}
