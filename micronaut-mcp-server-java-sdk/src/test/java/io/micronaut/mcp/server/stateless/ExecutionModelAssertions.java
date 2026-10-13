package io.micronaut.mcp.server.stateless;

import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.json.JsonMapper;
import io.modelcontextprotocol.spec.McpSchema;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static io.micronaut.mcp.server.stateless.ExecutionModelSupport.callTool;
import static io.micronaut.mcp.server.stateless.ExecutionModelSupport.callToolError;
import static io.micronaut.mcp.server.stateless.ExecutionModelSupport.complete;
import static io.micronaut.mcp.server.stateless.ExecutionModelSupport.text;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Assertions on {@link ExecutionModelTools} that hold in both execution models.
 */
final class ExecutionModelAssertions {
    private ExecutionModelAssertions() {
    }

    static void assertExecutionModel(HttpClient httpClient, JsonMapper jsonMapper) throws IOException {
        // results are dispatched on their value, not on the declared type
        assertEquals("wildcard", text(callTool(httpClient, jsonMapper, "wildcardMono")));
        assertEquals("{\"id\":\"1\",\"title\":\"Micronaut\",\"url\":\"https://micronaut.io\"}", text(callTool(httpClient, jsonMapper, "pojoAsObject")));
        // a publisher emitting more than one value fails instead of dropping values
        assertEquals(Boolean.TRUE, callTool(httpClient, jsonMapper, "twoValues").get("isError"));
        // an McpError is sent as is
        Map<String, Object> error = callToolError(httpClient, jsonMapper, "invalidParams", HttpStatus.BAD_REQUEST);
        assertEquals(McpSchema.ErrorCodes.INVALID_PARAMS, error.get("code"));
        assertEquals("bad params", error.get("message"));
        // the transport context is in the Reactor context
        assertEquals("true", text(callTool(httpClient, jsonMapper, "reactorContext")));
        // an empty publisher of structured content is a tool error
        Map<String, Object> empty = callTool(httpClient, jsonMapper, "emptyStructured");
        assertEquals(Boolean.TRUE, empty.get("isError"));
        assertEquals("Tool emptyStructured returned no structured content", text(empty));
        // @ExecuteOn is honoured, with the request in scope
        assertEquals("true true", text(callTool(httpClient, jsonMapper, "onIo")));
        // a publisher of completion values is collected
        assertEquals(List.of("Alice", "Bob"), complete(httpClient, jsonMapper, "greet"));
    }
}
