package io.micronaut.mcp.server.stateless;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.mcp.annotations.Prompt;
import io.micronaut.mcp.annotations.Resource;
import io.micronaut.mcp.annotations.Tool;
import io.micronaut.mcp.server.observability.McpServerObserver;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Property(name = "micronaut.mcp.server.info.name", value = "mcp-server")
@Property(name = "micronaut.mcp.server.info.version", value = "0.0.1")
@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "micronaut.mcp.server.reactive", value = "true")
@Property(name = "spec.name", value = "ReactiveOperationMetricsTest")
@MicronautTest
class ReactiveOperationMetricsTest {

    @Inject
    @Client("/")
    HttpClient httpClient;

    @Inject
    MeterRegistry meterRegistry;

    @Test
    void operationsAreTimedOnceWithTheirOutcome() {
        call("tools/call", """
            {"name": "add", "arguments": {"a": 1, "b": 2}}""");
        call("tools/call", """
            {"name": "add", "arguments": {"a": 1, "b": 2}}""");
        call("tools/call", """
            {"name": "broken", "arguments": {}}""");
        call("prompts/get", """
            {"name": "greet", "arguments": {}}""");
        callFailing("resources/read", """
            {"uri": "metrics://broken"}""");

        assertEquals(2, timer("tools/call", "add", "none").count());
        assertEquals(1, timer("tools/call", "broken", "tool_error").count());
        assertEquals(1, timer("prompts/get", "greet", "none").count());
        // an unmapped checked exception is an internal error
        assertEquals(1, timer("resources/read", "metrics://broken", "-32603").count());
        // the server cancels the result once it has its value, which is not recorded as a cancellation
        assertTrue(meterRegistry.find("mcp.server.operation.duration").tag("error.type", "cancelled").timers().isEmpty(),
            String.valueOf(meterRegistry.getMeters()));
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
        httpClient.toBlocking().retrieve(request(method, params));
    }

    private void callFailing(String method, String params) {
        HttpRequest<String> request = request(method, params);
        try {
            httpClient.toBlocking().retrieve(request);
        } catch (io.micronaut.http.client.exceptions.HttpClientResponseException _) {
            // a protocol error
        }
    }

    private static HttpRequest<String> request(String method, String params) {
        return HttpRequest.POST("/mcp", """
            {"jsonrpc": "2.0", "id": 1, "method": "%s", "params": %s}""".formatted(method, params));
    }

    @Requires(property = "spec.name", value = "ReactiveOperationMetricsTest")
    @Singleton
    static class Primitives {
        @Tool
        Mono<Integer> add(int a, int b) {
            return Mono.just(a + b);
        }

        @Tool
        String broken() {
            throw new IllegalStateException("broken");
        }

        @Prompt
        String greet() {
            return "Hello";
        }

        @Resource(uri = "metrics://broken")
        String brokenResource() throws IOException {
            throw new IOException("unreadable");
        }
    }

    /**
     * An observer that fails, which must neither fail the operations nor record them twice.
     */
    @Requires(property = "spec.name", value = "ReactiveOperationMetricsTest")
    @Singleton
    static class FailingObserver implements McpServerObserver {
        @Override
        public Observation start(String method, String name) {
            return errorType -> {
                throw new IllegalStateException("the observer is broken");
            };
        }
    }
}
