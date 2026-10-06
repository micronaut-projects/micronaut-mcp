package io.micronaut.mcp.server.stateless;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.sse.Event;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StreamingJsonRpcResponseTest {
    private static final McpSchema.JSONRPCNotification NOTIFICATION =
        new McpSchema.JSONRPCNotification(McpSchema.JSONRPC_VERSION, McpSchema.METHOD_NOTIFICATION_MESSAGE, null);
    private static final McpSchema.JSONRPCRequest REQUEST =
        new McpSchema.JSONRPCRequest(McpSchema.JSONRPC_VERSION, McpSchema.METHOD_TOOLS_CALL, 7, null);

    @Test
    void anErrorBeforeANotificationFailsTheResponse() {
        IllegalStateException error = new IllegalStateException("boom");
        Mono<HttpResponse<?>> response = Mono.create(sink -> new StreamingJsonRpcResponse(sink, REQUEST).fail(error));
        assertSame(error, assertThrows(IllegalStateException.class, response::block));
    }

    @Test
    void anErrorAfterANotificationIsAJsonRpcErrorEvent() {
        AtomicReference<StreamingJsonRpcResponse> streaming = new AtomicReference<>();
        HttpResponse<?> response = started(streaming);
        streaming.get().fail(new IllegalStateException("boom"));
        streaming.get().emit(NOTIFICATION);
        List<Event<?>> events = events(response).collectList().block();
        assertEquals(2, events.size());
        McpSchema.JSONRPCResponse error = assertInstanceOf(McpSchema.JSONRPCResponse.class, events.get(1).getData());
        assertEquals(7, error.id());
        assertEquals(McpSchema.ErrorCodes.INTERNAL_ERROR, error.error().code());
        assertEquals("Failed to handle request: boom", error.error().message());
    }

    @Test
    void anEmptyResultCompletesTheResponseEmpty() {
        assertNull(Mono.<HttpResponse<?>>create(sink -> new StreamingJsonRpcResponse(sink, REQUEST).completeEmpty()).block());
        AtomicReference<StreamingJsonRpcResponse> streaming = new AtomicReference<>();
        HttpResponse<?> response = started(streaming);
        streaming.get().completeEmpty();
        assertEquals(1, events(response).count().block());
    }

    @Test
    void cancellingTheStreamCancelsTheRequest() {
        AtomicReference<StreamingJsonRpcResponse> streaming = new AtomicReference<>();
        HttpResponse<?> response = started(streaming);
        Disposable subscription = Disposables.single();
        streaming.get().subscription(subscription);
        assertFalse(streaming.get().isCancelled());
        events(response).take(1).blockLast();
        assertTrue(streaming.get().isCancelled());
        assertTrue(subscription.isDisposed());
        // notifications of a cancelled request are dropped
        streaming.get().emit(NOTIFICATION);
    }

    @Test
    void cancellingBeforeTheFirstNotificationCancelsTheRequest() {
        AtomicReference<StreamingJsonRpcResponse> streaming = new AtomicReference<>();
        Disposable response = Mono.<HttpResponse<?>>create(sink -> streaming.set(new StreamingJsonRpcResponse(sink, REQUEST))).subscribe();
        Disposable subscription = Disposables.single();
        streaming.get().subscription(subscription);
        response.dispose();
        assertTrue(streaming.get().isCancelled());
        assertTrue(subscription.isDisposed());
        // a subscription that arrives after the client went away is disposed right away
        Disposable late = Disposables.single();
        streaming.get().subscription(late);
        assertTrue(late.isDisposed());
    }

    @Test
    void notificationsBeyondTheBufferAreDroppedButTheResultIsSent() {
        AtomicReference<StreamingJsonRpcResponse> streaming = new AtomicReference<>();
        HttpResponse<?> response = started(streaming);
        for (int i = 0; i < StreamingJsonRpcResponse.BUFFER_SIZE * 2; i++) {
            streaming.get().emit(NOTIFICATION);
        }
        streaming.get().complete(HttpResponse.ok("result"));
        List<Event<?>> events = events(response).collectList().block();
        assertEquals(StreamingJsonRpcResponse.BUFFER_SIZE, events.size());
        assertEquals("result", events.getLast().getData());
    }

    @Test
    void anEventStreamWithAZeroQualityIsNotAccepted() {
        assertTrue(McpStreamingPolicy.acceptsEventStream(HttpRequest.POST("/mcp", "").header("Accept", "application/json, text/event-stream")));
        assertTrue(McpStreamingPolicy.acceptsEventStream(HttpRequest.POST("/mcp", "").header("Accept", "text/event-stream;q=0.5")));
        assertFalse(McpStreamingPolicy.acceptsEventStream(HttpRequest.POST("/mcp", "").header("Accept", "application/json, text/event-stream;q=0")));
        assertFalse(McpStreamingPolicy.acceptsEventStream(HttpRequest.POST("/mcp", "").header("Accept", "text/event-stream; q=0.0")));
        assertFalse(McpStreamingPolicy.acceptsEventStream(HttpRequest.POST("/mcp", "").header("Accept", "application/json")));
    }

    private static HttpResponse<?> started(AtomicReference<StreamingJsonRpcResponse> streaming) {
        return Mono.<HttpResponse<?>>create(sink -> {
            streaming.set(new StreamingJsonRpcResponse(sink, REQUEST));
            streaming.get().emit(NOTIFICATION);
        }).block();
    }

    @SuppressWarnings("unchecked")
    private static Flux<Event<?>> events(HttpResponse<?> response) {
        return Flux.from((Publisher<Event<?>>) response.body());
    }
}
