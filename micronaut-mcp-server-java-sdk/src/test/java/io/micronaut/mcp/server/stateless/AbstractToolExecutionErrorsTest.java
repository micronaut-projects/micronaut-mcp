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
    @ValueSource(strings = {"failing", "failingPublisher", "failingOnExecutor", "failingAsync"})
    void aFailureWithoutAMapperIsAToolExecutionError(String tool) throws IOException {
        assertToolError("the backend is down", callTool(httpClient, jsonMapper, tool));
    }

    @ParameterizedTest
    @ValueSource(strings = {"failingWithoutMessage", "failingWithBlankMessage"})
    void aFailureWithoutAMessageIsDescribedByTheToolAndItsClass(String tool) throws IOException {
        assertToolError("Tool " + tool + " failed (IllegalStateException)", callTool(httpClient, jsonMapper, tool));
    }

    @Test
    void anInputErrorWithoutAMessageIsDescribedByTheToolAndItsClass() throws IOException {
        assertToolError("Tool failingInputErrorWithoutMessage failed (SerdeException)",
            callTool(httpClient, jsonMapper, "failingInputErrorWithoutMessage"));
    }

    @Test
    void aFailureOfAnAnonymousClassWithoutAMessageIsDescribedByTheClassName() throws IOException {
        Map<String, Object> result = callTool(httpClient, jsonMapper, "failingAnonymously");
        assertEquals(Boolean.TRUE, result.get("isError"));
        assertTrue(text(result).startsWith("Tool failingAnonymously failed (" + ToolErrorTools.class.getName() + "$"), text(result));
    }

    @ParameterizedTest
    @ValueSource(strings = {"checkedPublisher", "checkedFuture", "failingNested"})
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
    void anArgumentThatCannotBeBoundIsAToolExecutionError() throws IOException {
        Map<String, Object> result = callTool(httpClient, jsonMapper, "converted", """
            {"unit": "fortnights"}""");
        assertToolError("Required argument [TimeUnit unit] not specified", result);
    }

    @Test
    void anArgumentThatDoesNotMatchTheInputSchemaIsAToolExecutionError() throws IOException {
        Map<String, Object> result = callTool(httpClient, jsonMapper, "limited", """
            {"count": "eleven"}""");
        assertToolError("Tool (limited) input validation failed: /count: string found, number expected", result);
    }

    @Test
    void aMissingArgumentIsAToolExecutionError() throws IOException {
        assertToolError("Tool (limited) input validation failed: required property 'count' not found",
            callTool(httpClient, jsonMapper, "limited"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"protocolError", "protocolErrorFuture", "protocolErrorAsync"})
    void anMcpErrorIsAProtocolErrorWithItsCodeAndMessage(String tool) throws IOException {
        Map<String, Object> error = callToolError(httpClient, jsonMapper, tool, HttpStatus.BAD_REQUEST);
        assertEquals(McpSchema.ErrorCodes.INVALID_PARAMS, error.get("code"));
        assertEquals("unknown city", error.get("message"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"mapped", "mappedPublisher", "mappedAsync", "mappedCallable"})
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
        // the reason names the type that could not be serialized
        assertTrue(text(result).contains("Unserializable"), text(result));
    }

    private static void assertToolError(String message, Map<String, Object> result) {
        assertEquals(Boolean.TRUE, result.get("isError"), "isError");
        assertEquals(message, text(result));
    }
}
