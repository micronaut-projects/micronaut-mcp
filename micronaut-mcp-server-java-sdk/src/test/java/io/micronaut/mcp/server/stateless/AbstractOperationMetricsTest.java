package io.micronaut.mcp.server.stateless;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Calls the primitives of a server over HTTP and asserts the timers they produce. The sync and the reactive server
 * declare primitives with the same names and outcomes, each with the method signatures of its execution model.
 */
abstract class AbstractOperationMetricsTest {
    static final String METER = "mcp.server.operation.duration";
    static final long SLOW_MILLIS = 20;

    @Inject
    @Client("/")
    HttpClient httpClient;

    @Inject
    MeterRegistry meterRegistry;

    @Test
    void operationsAreTimedWithTheirOutcome() {
        call("tools/call", """
            {"name": "add", "arguments": {"a": 1, "b": 2}}""");
        call("tools/call", """
            {"name": "add", "arguments": {"a": 1, "b": 2}}""");
        call("tools/call", """
            {"name": "broken", "arguments": {}}""");
        callFailing("tools/call", """
            {"name": "invalid", "arguments": {}}""");
        call("tools/call", """
            {"name": "slow", "arguments": {}}""");
        for (int code : new int[] {1, 2, 3, 404, McpSchema.ErrorCodes.RESOURCE_NOT_FOUND}) {
            callFailing("tools/call", """
                {"name": "relay", "arguments": {"code": %d}}""".formatted(code));
        }
        callFailing("tools/call", """
            {"name": "unknown", "arguments": {}}""");
        call("prompts/get", """
            {"name": "greet", "arguments": {}}""");
        callFailing("prompts/get", """
            {"name": "brokenPrompt", "arguments": {}}""");
        callFailing("prompts/get", """
            {"name": "count", "arguments": {"n": "x"}}""");
        call("completion/complete", """
            {"ref": {"type": "ref/prompt", "name": "translate"}, "argument": {"name": "language", "value": "py"}}""");
        call("completion/complete", """
            {"ref": {"type": "ref/resource", "uri": "metrics://cities/{city}"}, "argument": {"name": "city", "value": "p"}}""");
        callFailing("completion/complete", """
            {"ref": {"type": "ref/prompt", "name": "unknown"}, "argument": {"name": "language", "value": "py"}}""");
        call("resources/read", """
            {"uri": "metrics://cities/paris"}""");
        call("resources/read", """
            {"uri": "metrics://cities/rome"}""");
        callFailing("resources/read", """
            {"uri": "metrics://broken"}""");

        String internalError = String.valueOf(McpSchema.ErrorCodes.INTERNAL_ERROR);
        String invalidParams = String.valueOf(McpSchema.ErrorCodes.INVALID_PARAMS);
        assertAll(
            () -> assertEquals(2, timer("tools/call", "add", "none").count()),
            // an exception of the tool is a tool execution error
            () -> assertEquals(1, timer("tools/call", "broken", "tool_error").count()),
            // a protocol error of the tool keeps its code
            () -> assertEquals(1, timer("tools/call", "invalid", invalidParams).count()),
            // codes the application defines, which may come from a remote peer, are reported as one error type
            () -> assertEquals(4, timer("tools/call", "relay", "_OTHER").count()),
            () -> assertEquals(1, timer("tools/call", "relay", String.valueOf(McpSchema.ErrorCodes.RESOURCE_NOT_FOUND)).count()),
            () -> assertEquals(2, meterRegistry.find(METER).tag("mcp.primitive.name", "relay").timers().size()),
            () -> assertTrue(timer("tools/call", "slow", "none").totalTime(TimeUnit.MILLISECONDS) >= SLOW_MILLIS,
                "the timer covers the operation until it completes"),
            () -> assertEquals(1, timer("prompts/get", "greet", "none").count()),
            // an unmapped exception of a prompt is an internal error
            () -> assertEquals(1, timer("prompts/get", "brokenPrompt", internalError).count()),
            // an argument that cannot be converted is invalid
            () -> assertEquals(1, timer("prompts/get", "count", invalidParams).count()),
            () -> assertEquals(1, timer("completion/complete", "translate", "none").count()),
            () -> assertEquals(1, timer("completion/complete", "metrics://cities/{city}", "none").count()),
            // a template read is named by its template, so the expanded URIs do not multiply the timers
            () -> assertEquals(2, timer("resources/read", "metrics://cities/{city}", "none").count()),
            // an unmapped checked exception is an internal error
            () -> assertEquals(1, timer("resources/read", "metrics://broken", internalError).count()),
            // requests the SDK rejects before dispatching them are not timed, so the names stay bounded
            () -> assertTrue(meterRegistry.find(METER).tag("mcp.primitive.name", "unknown").timers().isEmpty(),
                String.valueOf(meterRegistry.getMeters())),
            // the server cancels the result once it has its value, which is not recorded as a cancellation
            () -> assertTrue(meterRegistry.find(METER).tag("error.type", "cancelled").timers().isEmpty(),
                String.valueOf(meterRegistry.getMeters()))
        );
    }

    static McpError error(int code) {
        return new McpError(new McpSchema.JSONRPCResponse.JSONRPCError(code, "relayed", null));
    }

    static McpError invalidParams() {
        return new McpError(new McpSchema.JSONRPCResponse.JSONRPCError(McpSchema.ErrorCodes.INVALID_PARAMS, "invalid", null));
    }

    private Timer timer(String method, String name, String errorType) {
        Timer timer = meterRegistry.find(METER)
            .tag("mcp.method.name", method)
            .tag("mcp.primitive.name", name)
            .tag("error.type", errorType)
            .timer();
        assertNotNull(timer, method + " " + name + " " + errorType + " in " + meterRegistry.getMeters());
        return timer;
    }

    private void call(String method, String params) {
        httpClient.toBlocking().retrieve(request(method, params));
    }

    /**
     * Calls a method whose response may be a protocol error, which the server may answer with an HTTP error status.
     */
    private void callFailing(String method, String params) {
        HttpRequest<String> request = request(method, params);
        try {
            httpClient.toBlocking().retrieve(request);
        } catch (HttpClientResponseException _) {
            // a protocol error
        }
    }

    private static HttpRequest<String> request(String method, String params) {
        return HttpRequest.POST("/mcp", """
            {"jsonrpc": "2.0", "id": 1, "method": "%s", "params": %s}""".formatted(method, params));
    }
}
