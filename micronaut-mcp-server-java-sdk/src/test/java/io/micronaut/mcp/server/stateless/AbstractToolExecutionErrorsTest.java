package io.micronaut.mcp.server.stateless;

import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.json.JsonMapper;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.util.Map;

import static io.micronaut.mcp.server.stateless.ExecutionModelSupport.callTool;
import static io.micronaut.mcp.server.stateless.ExecutionModelSupport.callToolError;
import static io.micronaut.mcp.server.stateless.ExecutionModelSupport.text;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * How the failures of the {@link ToolErrorTools} are reported. The subclasses run these tests in each execution model.
 */
abstract class AbstractToolExecutionErrorsTest {

    @Inject
    @Client("/")
    HttpClient httpClient;

    @Inject
    JsonMapper jsonMapper;

    @ParameterizedTest
    @ValueSource(strings = {"failing", "failingPublisher"})
    void aFailureWithoutAMapperIsAToolExecutionError(String tool) throws IOException {
        assertToolError("the backend is down", callTool(httpClient, jsonMapper, tool));
    }

    @Test
    void aFailureWithoutAMessageIsDescribedByItsClass() throws IOException {
        assertToolError("IllegalStateException", callTool(httpClient, jsonMapper, "failingWithoutMessage"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"checkedPublisher", "checkedFuture"})
    void aCheckedExceptionIsUnwrapped(String tool) throws IOException {
        assertToolError("boom", callTool(httpClient, jsonMapper, tool));
    }

    @Test
    void anArgumentThatFailsValidationIsAToolExecutionError() throws IOException {
        Map<String, Object> result = callTool(httpClient, jsonMapper, "limited", """
            {"count": 11}""");
        assertToolError("count: must be less than or equal to 10", result);
    }

    @Test
    void anMcpErrorIsAProtocolErrorWithItsCodeAndMessage() throws IOException {
        Map<String, Object> error = callToolError(httpClient, jsonMapper, "protocolError", HttpStatus.BAD_REQUEST);
        assertEquals(McpSchema.ErrorCodes.INVALID_PARAMS, error.get("code"));
        assertEquals("unknown city", error.get("message"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"mapped", "mappedPublisher"})
    void aFailureThatAnApplicationMapperMapsIsAProtocolError(String tool) throws IOException {
        Map<String, Object> error = callToolError(httpClient, jsonMapper, tool, HttpStatus.BAD_REQUEST);
        assertEquals(McpSchema.ErrorCodes.INVALID_PARAMS, error.get("code"));
        assertEquals("no such city", error.get("message"));
    }

    @Test
    void aResultThatCannotBeSerializedIsAToolExecutionError() throws IOException {
        Map<String, Object> result = callTool(httpClient, jsonMapper, "unserializable");
        assertEquals(Boolean.TRUE, result.get("isError"));
        assertTrue(text(result).startsWith("The result of tool unserializable could not be serialized: "), text(result));
    }

    private static void assertToolError(String message, Map<String, Object> result) {
        assertEquals(Boolean.TRUE, result.get("isError"), "isError");
        assertEquals(message, text(result));
    }
}
