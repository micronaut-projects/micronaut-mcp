package io.micronaut.mcp.server.stateless;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.mcp.annotations.Prompt;
import io.micronaut.mcp.annotations.Tool;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Property(name = "micronaut.mcp.server.info.name", value = "mcp-server")
@Property(name = "micronaut.mcp.server.info.version", value = "0.0.1")
@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "spec.name", value = "OperationMetricsTest")
@MicronautTest
class OperationMetricsTest {

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
        call("prompts/get", """
            {"name": "greet", "arguments": {}}""");

        assertEquals(2, timer("tools/call", "add", "none").count());
        assertEquals(1, timer("tools/call", "broken", "tool_error").count());
        assertEquals(1, timer("prompts/get", "greet", "none").count());
    }

    private Timer timer(String method, String name, String errorType) {
        Timer timer = meterRegistry.find("mcp.server.operation.duration")
            .tag("mcp.method.name", method)
            .tag("mcp.primitive.name", name)
            .tag("error.type", errorType)
            .timer();
        assertNotNull(timer, method + " " + name + " " + errorType + " in " + meterRegistry.getMeters());
        return timer;
    }

    private void call(String method, String params) {
        httpClient.toBlocking().retrieve(HttpRequest.POST("/mcp", """
            {"jsonrpc": "2.0", "id": 1, "method": "%s", "params": %s}""".formatted(method, params)));
    }

    @Requires(property = "spec.name", value = "OperationMetricsTest")
    @Singleton
    static class Primitives {
        @Tool
        int add(int a, int b) {
            return a + b;
        }

        @Tool
        String broken() {
            throw new IllegalStateException("broken");
        }

        @Prompt
        String greet() {
            return "Hello";
        }
    }
}
