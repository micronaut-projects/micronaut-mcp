/*
 * Copyright 2017-2026 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.mcp.server.stateless;

import io.micronaut.core.annotation.Internal;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.sse.Event;
import io.micronaut.mcp.server.context.McpNotificationEmitter;
import io.micronaut.mcp.server.exceptions.JsonRrpcResponseUtils;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;
import reactor.core.publisher.MonoSink;
import reactor.core.publisher.Sinks;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * The response to a JSON-RPC request whose client accepts both JSON and an event stream. It stays a JSON response unless
 * the request sends a notification before its result, in which case it becomes an event stream of the notifications
 * followed by the result.
 *
 * <p>Once the response is an event stream, a client that closes it cancels the stream: the subscription to the result is
 * disposed, {@link #isCancelled()} becomes {@code true} and further notifications are dropped. Cancelling the response
 * before the first notification does the same, but the HTTP server does not cancel a response it has not received yet
 * when the client goes away, so in practice only the event stream is cancelled.</p>
 *
 * @see <a href="https://modelcontextprotocol.io/specification/2025-11-25/basic/transports#sending-messages-to-the-server">Sending messages to the server</a>
 */
@Internal
final class StreamingJsonRpcResponse implements McpNotificationEmitter {
    /**
     * The number of events a slow client may have pending. Notifications beyond it are dropped, but there is always room
     * for the final result.
     */
    static final int BUFFER_SIZE = 256;
    private static final Logger LOG = LoggerFactory.getLogger(StreamingJsonRpcResponse.class);

    private final MonoSink<HttpResponse<?>> response;
    private final McpSchema.JSONRPCRequest request;
    private @Nullable BlockingQueue<Event<Object>> queue;
    private Sinks.@Nullable Many<Event<Object>> events;
    private @Nullable Disposable subscription;
    private boolean finished;
    private boolean overflowed;
    private volatile boolean cancelled;

    StreamingJsonRpcResponse(MonoSink<HttpResponse<?>> response, McpSchema.JSONRPCRequest request) {
        this.response = response;
        this.request = request;
        // Only cancels the response itself: once it succeeds with the stream, cancelling the stream cancels the request
        response.onCancel(this::cancel);
    }

    /**
     * @param subscription The subscription to the result of the request, disposed if the client goes away
     */
    synchronized void subscription(Disposable subscription) {
        if (cancelled) {
            subscription.dispose();
        } else {
            this.subscription = subscription;
        }
    }

    @Override
    public void emit(McpSchema.JSONRPCNotification notification) {
        HttpResponse<?> stream = null;
        synchronized (this) {
            if (finished || cancelled) {
                return;
            }
            if (events == null) {
                stream = startStream();
            }
            // Keeps a slot for the result
            if (queue.remainingCapacity() > 1) {
                emitEvent(notification);
            } else {
                dropped(notification);
            }
        }
        if (stream != null) {
            // Outside the lock: the server writes the response, and runs its filters, on this thread. The unicast sink
            // keeps the events emitted until then.
            response.success(stream);
        }
    }

    private static String message(Throwable error) {
        String message = error.getMessage();
        return message != null && !message.isBlank() ? message : error.getClass().getSimpleName();
    }

    private void dropped(McpSchema.JSONRPCNotification notification) {
        if (!overflowed) {
            overflowed = true;
            if (LOG.isWarnEnabled()) {
                LOG.warn("Dropping the notifications of request {}: the client does not read the event stream fast enough, {} events are pending",
                    request.id(), BUFFER_SIZE - 1);
            }
        } else if (LOG.isDebugEnabled()) {
            LOG.debug("Dropped a {} notification: the client does not read the event stream fast enough", notification.method());
        }
    }

    private HttpResponse<?> startStream() {
        queue = new ArrayBlockingQueue<>(BUFFER_SIZE);
        events = Sinks.many().unicast().onBackpressureBuffer(queue);
        return HttpResponse.ok(events.asFlux().doOnCancel(this::cancel))
            .contentType(MediaType.TEXT_EVENT_STREAM_TYPE);
    }

    private void emitEvent(Object data) {
        Sinks.EmitResult result = events.tryEmitNext(Event.of(data));
        if (result.isFailure() && LOG.isDebugEnabled()) {
            LOG.debug("Failed to send an event to the client: {}", result);
        }
    }

    private void completeEvents() {
        Sinks.EmitResult result = events.tryEmitComplete();
        if (result.isFailure() && LOG.isDebugEnabled()) {
            LOG.debug("Failed to end the event stream: {}", result);
        }
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    private void cancel() {
        Disposable toDispose;
        synchronized (this) {
            if (cancelled || finished) {
                return;
            }
            cancelled = true;
            toDispose = subscription;
        }
        if (toDispose != null) {
            toDispose.dispose();
        }
    }

    /**
     * Marks the response finished.
     *
     * @return Whether the response is still JSON, so it is for the caller to complete, outside the lock; otherwise the
     * event stream has started, and the response is already completed with it
     */
    private boolean finishJson() {
        finished = true;
        return events == null;
    }

    /**
     * Completes the response with the result of the request.
     *
     * @param result The response the request would have had as JSON
     */
    void complete(HttpResponse<?> result) {
        synchronized (this) {
            if (finished) {
                return;
            }
            if (!finishJson()) {
                result.getBody().ifPresent(this::emitEvent);
                completeEvents();
                return;
            }
        }
        response.success(result);
    }

    /**
     * Completes the response when the request produced no result, as the JSON response does: with no response before
     * a notification, or by ending the event stream after one.
     */
    void completeEmpty() {
        synchronized (this) {
            if (finished) {
                return;
            }
            if (!finishJson()) {
                completeEvents();
                return;
            }
        }
        response.success();
    }

    /**
     * Completes the response with an error the request did not turn into a JSON-RPC response. Once the event stream has
     * started, the error is sent as a JSON-RPC error response event, so the client gets an answer to its request.
     * {@code McpController} turns every error of a request into a JSON-RPC response, so this is a fallback that should
     * not normally run.
     *
     * @param error The error
     */
    void fail(Throwable error) {
        synchronized (this) {
            if (finished) {
                return;
            }
            if (!finishJson()) {
                McpError mcpError = error instanceof McpError e ? e
                    : McpError.builder(McpSchema.ErrorCodes.INTERNAL_ERROR).message("Failed to handle request: " + message(error)).build();
                emitEvent(JsonRrpcResponseUtils.jsonrpcResponse(mcpError, request));
                completeEvents();
                return;
            }
        }
        response.error(error);
    }
}
