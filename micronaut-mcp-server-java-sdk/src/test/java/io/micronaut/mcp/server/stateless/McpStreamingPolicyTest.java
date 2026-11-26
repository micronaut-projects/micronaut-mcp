package io.micronaut.mcp.server.stateless;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.json.JsonMapper;
import io.micronaut.json.tree.JsonNode;
import io.micronaut.mcp.annotations.ResourceTemplate;
import io.micronaut.mcp.annotations.Tool;
import io.micronaut.mcp.server.context.McpRequestContext;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Property(name = "micronaut.mcp.server.info.name", value = "mcp-server")
@Property(name = "micronaut.mcp.server.info.version", value = "0.0.1")
@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "spec.name", value = "McpStreamingPolicyTest")
@MicronautTest(startApplication = false)
class McpStreamingPolicyTest {
    private static final String ACCEPT_BOTH = "application/json, text/event-stream";
    private static final String CALL_NOTIFYING = """
        {"jsonrpc": "2.0", "id": 1, "method": "tools/call", "params": {"name": "notifying", "arguments": {}}}""";

    @Inject
    McpStreamingPolicy policy;

    @Inject
    JsonMapper jsonMapper;

    // The Streamable HTTP transport requires a POST request to list both JSON and an event stream
    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
        application/json, text/event-stream                | true
        text/event-stream, application/json                | true
        APPLICATION/JSON, TEXT/EVENT-STREAM                | true
        application/json;q=0.9, text/event-stream;q=0.5    | true
        application/json, text/event-stream;level=1;q=0.1  | true
        application/json, text/event-stream;level=1        | true
        text/event-stream                                  | false
        application/json                                   | false
        application/json, text/event-stream;q=0            | false
        application/json, text/event-stream; q=0.0         | false
        application/json;q=0, text/event-stream            | false
        application/json, text/event-stream;level=1;q=0    | false
        application/json, text/event-stream;q=abc          | false
        */*                                                | false
        application/json, text/*                           | false
        """)
    void theClientMustAcceptJsonAndAnEventStream(String accept, boolean accepted) {
        assertEquals(accepted, McpStreamingPolicy.acceptsJsonAndEventStream(HttpRequest.POST("/mcp", "").header("Accept", accept)), accept);
    }

    @Test
    void theAcceptHeaderMayBeRepeated() {
        MutableHttpRequest<String> request = HttpRequest.POST("/mcp", "");
        request.getHeaders().add("Accept", "application/json").add("Accept", "text/event-stream");
        assertTrue(McpStreamingPolicy.acceptsJsonAndEventStream(request));
        assertFalse(McpStreamingPolicy.acceptsJsonAndEventStream(HttpRequest.POST("/mcp", "")), "without an Accept header");
    }

    @Test
    void aCallOfAPrimitiveWithARequestContextMayStream() throws IOException {
        assertTrue(policy.mayStream(request(ACCEPT_BOTH), body(CALL_NOTIFYING)));
        assertFalse(policy.mayStream(request("text/event-stream"), body(CALL_NOTIFYING)), "a client that does not accept JSON");
    }

    @Test
    void aResourceReadMayStreamOnlyForAClientThatAcceptsBoth() throws IOException {
        JsonNode read = body("""
            {"jsonrpc": "2.0", "id": 1, "method": "resources/read", "params": {"uri": "items://42"}}""");
        assertTrue(policy.mayStream(request(ACCEPT_BOTH), read));
        assertFalse(policy.mayStream(request("application/json"), read));
        assertFalse(policy.mayStream(request(ACCEPT_BOTH), body("""
            {"jsonrpc": "2.0", "id": 1, "method": "resources/read", "params": {"uri": "other://42"}}""")));
        assertFalse(policy.mayStream(request(ACCEPT_BOTH), body("""
            {"jsonrpc": "2.0", "id": 1, "method": "resources/read", "params": {"uri": 42}}""")));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "[]",
        "{\"jsonrpc\": \"2.0\", \"id\": 1, \"params\": {\"name\": \"notifying\"}}",
        "{\"jsonrpc\": \"2.0\", \"id\": 1, \"method\": 1, \"params\": {\"name\": \"notifying\"}}",
        "{\"jsonrpc\": \"2.0\", \"id\": 1, \"method\": \"tools/call\"}",
        "{\"jsonrpc\": \"2.0\", \"id\": 1, \"method\": \"tools/call\", \"params\": []}",
        "{\"jsonrpc\": \"2.0\", \"id\": 1, \"method\": \"tools/call\", \"params\": {\"name\": 1}}",
        "{\"jsonrpc\": \"2.0\", \"id\": 1, \"method\": \"tools/call\", \"params\": {\"name\": \"quiet\"}}",
        "{\"jsonrpc\": \"2.0\", \"id\": 1, \"method\": \"tools/list\", \"params\": {\"name\": \"notifying\"}}",
        "{\"jsonrpc\": \"2.0\", \"id\": 1, \"method\": \"completion/complete\", \"params\": {}}"
    })
    void otherRequestsDoNotStream(String body) throws IOException {
        assertFalse(policy.mayStream(request(ACCEPT_BOTH), body(body)), body);
    }

    @Test
    void aRequestWithoutABodyDoesNotStream() {
        assertFalse(policy.mayStream(request(ACCEPT_BOTH), null));
    }

    private static HttpRequest<?> request(String accept) {
        return HttpRequest.POST("/mcp", "").header("Accept", accept);
    }

    private JsonNode body(String json) throws IOException {
        return jsonMapper.readValue(json, JsonNode.class);
    }

    @Requires(property = "spec.name", value = "McpStreamingPolicyTest")
    @Singleton
    static class Tools {
        @Tool
        String notifying(McpRequestContext context) {
            return "notifying";
        }

        @Tool
        String quiet() {
            return "quiet";
        }

        @ResourceTemplate(uriTemplate = "items://{id}")
        String item(String id, McpRequestContext context) {
            return id;
        }
    }
}
