package io.micronaut.mcp.server.registry;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.mcp.server.observability.McpServerObserver;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Observes operations through the helpers of the registries, next to an observer that fails to start, so that the outcomes
 * the HTTP tests cannot produce are covered: cancellations, errors that are not exceptions, and failing error type mappings.
 */
@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "spec.name", value = "ObservedOperationsTest")
@MicronautTest(startApplication = false)
class ObservedOperationsTest {

    @Inject
    PromptRegistry registry;

    @Inject
    RecordingObserver observer;

    @BeforeEach
    void clear() {
        observer.stops.clear();
    }

    @Test
    void aCancelledOperationIsRecordedAsCancelled() {
        registry.observedAsync("prompts/get", "slow", Mono::never).subscribe().dispose();

        assertEquals(List.of("prompts/get slow " + McpServerObserver.CANCELLED), observer.stops);
    }

    @Test
    void aValueIsRecordedAsASuccess() {
        assertEquals("value", registry.observedAsync("prompts/get", "fast", () -> Mono.just("value")).block());

        assertEquals(List.of("prompts/get fast null"), observer.stops);
    }

    @Test
    void aProtocolErrorIsRecordedWithItsCode() {
        McpError error = new McpError(new McpSchema.JSONRPCResponse.JSONRPCError(McpSchema.ErrorCodes.INVALID_PARAMS, "invalid", null));

        assertSame(error, assertThrows(McpError.class,
            () -> registry.observedAsync("prompts/get", "invalid", () -> Mono.error(error)).block()));
        assertSame(error, assertThrows(McpError.class,
            () -> registry.observed("prompts/get", "invalid", () -> {
                throw error;
            })));

        String stop = "prompts/get invalid " + McpSchema.ErrorCodes.INVALID_PARAMS;
        assertEquals(List.of(stop, stop), observer.stops);
    }

    @Test
    void anErrorIsRecordedWithItsClassAndRethrownUnchanged() {
        LinkageError error = new LinkageError("broken");

        assertSame(error, assertThrows(LinkageError.class, () -> registry.observed("prompts/get", "broken", () -> {
            throw error;
        })));

        assertEquals(List.of("prompts/get broken java.lang.LinkageError"), observer.stops);
    }

    @Test
    void anErrorThrownWhileAssemblingIsRecordedWithItsClassAndRethrownUnchanged() {
        AssertionError error = new AssertionError("broken");

        Throwable thrown = assertThrows(Throwable.class, () -> registry.observedAsync("prompts/get", "assembling", () -> {
            throw error;
        }).block());

        // block() wraps a checked exception or an Error that is not fatal to the JVM
        assertSame(error, Exceptions.unwrap(thrown));
        assertEquals(List.of("prompts/get assembling java.lang.AssertionError"), observer.stops);
    }

    @Test
    void aFailingErrorTypeMappingRecordsASuccess() {
        assertEquals("value", registry.observed("prompts/get", "mapped", () -> "value", result -> {
            throw new IllegalStateException("the mapping is broken");
        }));
        assertEquals("value", registry.observedAsync("prompts/get", "mapped", () -> Mono.just("value"), result -> {
            throw new IllegalStateException("the mapping is broken");
        }).block());

        assertEquals(List.of("prompts/get mapped null", "prompts/get mapped null"), observer.stops);
    }

    @Requires(property = "spec.name", value = "ObservedOperationsTest")
    @Singleton
    static class RecordingObserver implements McpServerObserver {
        final List<String> stops = Collections.synchronizedList(new ArrayList<>());

        @Override
        public Observation start(String method, String name) {
            return errorType -> stops.add(method + " " + name + " " + errorType);
        }
    }

    /**
     * An observer that fails to start, which must neither fail the operations nor keep the other observers from recording them.
     */
    @Requires(property = "spec.name", value = "ObservedOperationsTest")
    @Singleton
    static class FailingObserver implements McpServerObserver {
        @Override
        public Observation start(String method, String name) {
            throw new IllegalStateException("the observer is broken");
        }
    }
}
