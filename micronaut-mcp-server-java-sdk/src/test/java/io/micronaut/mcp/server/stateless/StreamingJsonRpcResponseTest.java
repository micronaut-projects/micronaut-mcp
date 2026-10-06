package io.micronaut.mcp.server.stateless;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.sse.Event;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StreamingJsonRpcResponseTest {
    private static final McpSchema.JSONRPCNotification NOTIFICATION =
        new McpSchema.JSONRPCNotification(McpSchema.JSONRPC_VERSION, McpSchema.METHOD_NOTIFICATION_MESSAGE, null);

    @Test
    void anErrorBeforeANotificationFailsTheResponse() {
        IllegalStateException error = new IllegalStateException("boom");
        Mono<HttpResponse<?>> response = Mono.create(sink -> new StreamingJsonRpcResponse(sink).fail(error));
        assertSame(error, assertThrows(IllegalStateException.class, response::block));
    }

    @Test
    void anErrorAfterANotificationFailsTheStream() {
        AtomicReference<StreamingJsonRpcResponse> streaming = new AtomicReference<>();
        HttpResponse<?> response = Mono.<HttpResponse<?>>create(sink -> {
            streaming.set(new StreamingJsonRpcResponse(sink));
            streaming.get().emit(NOTIFICATION);
        }).block();
        streaming.get().fail(new IllegalStateException("boom"));
        streaming.get().emit(NOTIFICATION);
        Flux<Event<?>> events = events(response);
        assertEquals(1, events.onErrorComplete().count().block());
        assertThrows(IllegalStateException.class, () -> events(response).blockLast());
    }

    @Test
    void cancellingTheStreamCancelsTheRequest() {
        AtomicReference<StreamingJsonRpcResponse> streaming = new AtomicReference<>();
        HttpResponse<?> response = Mono.<HttpResponse<?>>create(sink -> {
            streaming.set(new StreamingJsonRpcResponse(sink));
            streaming.get().emit(NOTIFICATION);
        }).block();
        assertFalse(streaming.get().isCancelled());
        events(response).take(1).blockLast();
        assertTrue(streaming.get().isCancelled());
        // notifications of a cancelled request are dropped
        streaming.get().emit(NOTIFICATION);
    }

    @SuppressWarnings("unchecked")
    private static Flux<Event<?>> events(HttpResponse<?> response) {
        return Flux.from((Publisher<Event<?>>) response.body());
    }
}
