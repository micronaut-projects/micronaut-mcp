package io.micronaut.mcp.server.stateless;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.sse.Event;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;
import reactor.util.context.Context;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.LongConsumer;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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
    void anMcpErrorAfterANotificationKeepsItsCode() {
        AtomicReference<StreamingJsonRpcResponse> streaming = new AtomicReference<>();
        HttpResponse<?> response = started(streaming);
        streaming.get().fail(McpError.builder(McpSchema.ErrorCodes.INVALID_PARAMS).message("bad").build());
        McpSchema.JSONRPCResponse error = assertInstanceOf(McpSchema.JSONRPCResponse.class,
            events(response).collectList().block().getLast().getData());
        assertEquals(McpSchema.ErrorCodes.INVALID_PARAMS, error.error().code());
        assertEquals("bad", error.error().message());
    }

    @Test
    void anErrorWithoutAMessageIsNamedByItsType() {
        AtomicReference<StreamingJsonRpcResponse> streaming = new AtomicReference<>();
        HttpResponse<?> response = started(streaming);
        streaming.get().fail(new IllegalStateException());
        McpSchema.JSONRPCResponse error = assertInstanceOf(McpSchema.JSONRPCResponse.class,
            events(response).collectList().block().getLast().getData());
        assertEquals("Failed to handle request: IllegalStateException", error.error().message());
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
        // the client no longer reads the stream, so the result cannot be sent, and nothing fails
        assertDoesNotThrow(() -> streaming.get().complete(HttpResponse.ok("late")));
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
    void largeNotificationsBeyondTheSizeBudgetAreDroppedButTheResultIsSent() {
        AtomicReference<StreamingJsonRpcResponse> streaming = new AtomicReference<>();
        HttpResponse<?> response = started(streaming);
        McpSchema.JSONRPCNotification large = log("x".repeat(10_000));
        long size = StreamingJsonRpcResponse.estimatedSize(large);
        for (int i = 0; i < 200; i++) {
            streaming.get().emit(large);
        }
        streaming.get().complete(HttpResponse.ok("result"));
        List<Event<?>> events = events(response).collectList().block();
        // the first, small notification, as many large ones as fit in the budget, and the result
        long fitting = (StreamingJsonRpcResponse.BUFFER_CHARS - StreamingJsonRpcResponse.estimatedSize(NOTIFICATION)) / size;
        assertEquals(1 + fitting + 1, events.size());
        assertEquals("result", events.getLast().getData());
    }

    @Test
    void notificationsTheClientReadFreeTheSizeBudget() {
        AtomicReference<StreamingJsonRpcResponse> streaming = new AtomicReference<>();
        HttpResponse<?> response = started(streaming);
        List<Event<?>> received = new CopyOnWriteArrayList<>();
        // the client reads each event as it is sent
        events(response).subscribe(received::add);
        McpSchema.JSONRPCNotification large = log("x".repeat(100_000));
        for (int i = 0; i < 50; i++) {
            streaming.get().emit(large);
        }
        streaming.get().complete(HttpResponse.ok("result"));
        assertEquals(1 + 50 + 1, received.size());
    }

    @Test
    void aNotificationLargerThanTheBudgetIsSentWhenNoneIsPending() {
        AtomicReference<StreamingJsonRpcResponse> streaming = new AtomicReference<>();
        HttpResponse<?> response = started(streaming);
        List<Event<?>> received = new CopyOnWriteArrayList<>();
        events(response).subscribe(received::add);
        streaming.get().emit(log("x".repeat((int) StreamingJsonRpcResponse.BUFFER_CHARS * 2)));
        streaming.get().complete(HttpResponse.ok("result"));
        assertEquals(3, received.size());
    }

    @Test
    void theSizeOfANotificationIsEstimatedFromItsTexts() {
        assertEquals(128, StreamingJsonRpcResponse.estimatedSize(NOTIFICATION));
        assertEquals(128 + 3 + 7, StreamingJsonRpcResponse.estimatedSize(new McpSchema.JSONRPCNotification(McpSchema.JSONRPC_VERSION,
            McpSchema.METHOD_NOTIFICATION_MESSAGE, new McpSchema.LoggingMessageNotification(McpSchema.LoggingLevel.INFO, "app", "message", null))));
        assertEquals(128 + 4 + 5, StreamingJsonRpcResponse.estimatedSize(new McpSchema.JSONRPCNotification(McpSchema.JSONRPC_VERSION,
            McpSchema.METHOD_NOTIFICATION_PROGRESS, new McpSchema.ProgressNotification("token", 1.0, null, "half", null))));
        assertEquals(128, StreamingJsonRpcResponse.estimatedSize(new McpSchema.JSONRPCNotification(McpSchema.JSONRPC_VERSION,
            McpSchema.METHOD_NOTIFICATION_PROGRESS, new McpSchema.ProgressNotification(7, 1.0, null, null, null))));
    }

    private static McpSchema.JSONRPCNotification log(String message) {
        return new McpSchema.JSONRPCNotification(McpSchema.JSONRPC_VERSION, McpSchema.METHOD_NOTIFICATION_MESSAGE,
            new McpSchema.LoggingMessageNotification(McpSchema.LoggingLevel.INFO, null, message, null));
    }

    @Test
    void concurrentNotificationsAndResultProduceOneResponseWithTheResultLast() throws Exception {
        int threads = 8;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        try {
            for (int run = 0; run < 200; run++) {
                RecordingSink sink = new RecordingSink();
                StreamingJsonRpcResponse streaming = new StreamingJsonRpcResponse(sink, REQUEST);
                CountDownLatch start = new CountDownLatch(1);
                List<Future<?>> emitters = new ArrayList<>();
                for (int t = 0; t < threads; t++) {
                    emitters.add(executor.submit(() -> {
                        start.await();
                        for (int i = 0; i < 50; i++) {
                            streaming.emit(NOTIFICATION);
                        }
                        return null;
                    }));
                }
                start.countDown();
                streaming.complete(HttpResponse.ok("result"));
                for (Future<?> emitter : emitters) {
                    emitter.get(10, TimeUnit.SECONDS);
                }
                assertEquals(1, sink.responses.size(), "exactly one response, run " + run);
                Object body = sink.responses.getFirst().body();
                if (body instanceof Publisher<?>) {
                    List<Event<?>> events = events(sink.responses.getFirst()).collectList().block(Duration.ofSeconds(10));
                    assertTrue(events.size() <= StreamingJsonRpcResponse.BUFFER_SIZE, "run " + run);
                    assertEquals("result", events.getLast().getData(), "the result is last, run " + run);
                    assertEquals(1, events.stream().filter(e -> "result".equals(e.getData())).count(), "run " + run);
                } else {
                    // The result came before any notification, so the response is JSON
                    assertEquals("result", body, "run " + run);
                }
            }
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * Records every response the streaming response completes with, which the sink of {@code Mono.create} would not
     * show beyond the first.
     */
    private static final class RecordingSink implements MonoSink<HttpResponse<?>> {
        private final List<HttpResponse<?>> responses = new CopyOnWriteArrayList<>();

        @Override
        public void success() {
            responses.add(HttpResponse.noContent());
        }

        @Override
        public void success(HttpResponse<?> value) {
            responses.add(value);
        }

        @Override
        public void error(Throwable e) {
            throw new AssertionError("unexpected error", e);
        }

        @Override
        @Deprecated
        public Context currentContext() {
            return Context.empty();
        }

        @Override
        public MonoSink<HttpResponse<?>> onRequest(LongConsumer consumer) {
            return this;
        }

        @Override
        public MonoSink<HttpResponse<?>> onCancel(Disposable disposable) {
            return this;
        }

        @Override
        public MonoSink<HttpResponse<?>> onDispose(Disposable disposable) {
            return this;
        }
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
