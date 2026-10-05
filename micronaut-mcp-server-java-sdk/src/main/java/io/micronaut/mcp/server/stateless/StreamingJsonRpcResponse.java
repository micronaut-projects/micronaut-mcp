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
import io.modelcontextprotocol.spec.McpSchema;
import reactor.core.publisher.MonoSink;
import reactor.core.publisher.Sinks;

/**
 * The response to a JSON-RPC request whose client accepts both JSON and an event stream. It stays a JSON response unless
 * the request sends a notification before its result, in which case it becomes an event stream of the notifications
 * followed by the result.
 *
 * @see <a href="https://modelcontextprotocol.io/specification/2025-11-25/basic/transports#sending-messages-to-the-server">Sending messages to the server</a>
 */
@Internal
final class StreamingJsonRpcResponse implements McpNotificationEmitter {
    private final MonoSink<HttpResponse<?>> response;
    private Sinks.Many<Event<Object>> events;
    private boolean finished;
    private volatile boolean cancelled;

    StreamingJsonRpcResponse(MonoSink<HttpResponse<?>> response) {
        this.response = response;
    }

    @Override
    public synchronized void emit(McpSchema.JSONRPCNotification notification) {
        if (finished || cancelled) {
            return;
        }
        if (events == null) {
            events = Sinks.many().unicast().onBackpressureBuffer();
            HttpResponse<?> stream = HttpResponse.ok(events.asFlux().doOnCancel(() -> cancelled = true))
                .contentType(MediaType.TEXT_EVENT_STREAM_TYPE);
            response.success(stream);
        }
        events.tryEmitNext(Event.of(notification));
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * Completes the response with the result of the request.
     *
     * @param result The response the request would have had as JSON
     */
    synchronized void complete(HttpResponse<?> result) {
        finished = true;
        if (events == null) {
            response.success(result);
        } else {
            result.getBody().ifPresent(body -> events.tryEmitNext(Event.of(body)));
            events.tryEmitComplete();
        }
    }

    /**
     * Completes the response with an error the request did not turn into a JSON-RPC response.
     *
     * @param error The error
     */
    synchronized void fail(Throwable error) {
        finished = true;
        if (events == null) {
            response.error(error);
        } else {
            events.tryEmitError(error);
        }
    }
}
