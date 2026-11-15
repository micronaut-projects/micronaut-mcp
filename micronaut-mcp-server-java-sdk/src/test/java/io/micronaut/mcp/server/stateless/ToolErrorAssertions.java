package io.micronaut.mcp.server.stateless;

import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.json.JsonMapper;
import io.modelcontextprotocol.spec.McpSchema;

import java.io.IOException;
import java.util.Map;

import static io.micronaut.mcp.server.stateless.ExecutionModelSupport.callTool;
import static io.micronaut.mcp.server.stateless.ExecutionModelSupport.callToolError;
import static io.micronaut.mcp.server.stateless.ExecutionModelSupport.text;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Assertions on {@link ToolErrorTools} that hold in both execution models.
 */
final class ToolErrorAssertions {
    private ToolErrorAssertions() {
    }

    static void assertToolErrors(HttpClient httpClient, JsonMapper jsonMapper) throws IOException {
        // checked exceptions are unwrapped, so the message is the same in both execution models
        assertToolError("boom", callTool(httpClient, jsonMapper, "checkedPublisher"));
        assertToolError("boom", callTool(httpClient, jsonMapper, "checkedFuture"));
        // an exception that an application mapper maps is a protocol error with the mapped code
        for (String tool : new String[] {"mapped", "mappedPublisher"}) {
            Map<String, Object> error = callToolError(httpClient, jsonMapper, tool, HttpStatus.BAD_REQUEST);
            assertEquals(McpSchema.ErrorCodes.INVALID_PARAMS, error.get("code"));
            assertEquals("no such city", error.get("message"));
        }
        // a result that cannot be serialized is a tool execution error with the reason
        Map<String, Object> unserializable = callTool(httpClient, jsonMapper, "unserializable");
        assertEquals(Boolean.TRUE, unserializable.get("isError"));
        assertTrue(text(unserializable).startsWith("The result of tool unserializable could not be serialized: "), text(unserializable));
    }

    private static void assertToolError(String message, Map<String, Object> result) {
        assertEquals(Boolean.TRUE, result.get("isError"));
        assertEquals(message, text(result));
    }
}
