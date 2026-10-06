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
package io.micronaut.mcp.client.javasdk;

import io.micronaut.core.annotation.Internal;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.http.HttpStatus;
import io.micronaut.mcp.client.http.McpHttpSessionNotFoundException;
import io.micronaut.mcp.client.http.MicronautMcpHttpExchange;
import io.micronaut.mcp.conf.client.McpClientHeadersProvider;
import io.micronaut.mcp.conf.client.McpClientHttpConfiguration;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.TypeRef;
import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpTransportSessionNotFoundException;
import io.modelcontextprotocol.spec.ProtocolVersions;
import org.jspecify.annotations.Nullable;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * A Java SDK MCP client transport over Streamable HTTP, on the Micronaut HTTP client. Once the session is initialized,
 * it listens to the stream of the messages the server sends on its own, reconnecting with a backoff, unless the server
 * does not offer one.
 */
@Internal
final class MicronautStreamableHttpClientTransport implements McpClientTransport {
    private static final List<String> PROTOCOL_VERSIONS = List.of(ProtocolVersions.MCP_2025_03_26,
        ProtocolVersions.MCP_2025_06_18, ProtocolVersions.MCP_2025_11_25);
    private static final Duration RECONNECT_DELAY = Duration.ofSeconds(1);
    private static final Duration MAX_RECONNECT_DELAY = Duration.ofSeconds(30);
    private static final TypeRef<Map<String, Object>> MAP_TYPE = new TypeRef<>() {
    };

    private final MicronautMcpHttpExchange exchange;
    private final McpClientHttpConfiguration configuration;
    private final List<McpClientHeadersProvider> headersProviders;
    private final McpJsonMapper jsonMapper;
    private final AtomicReference<Function<Mono<McpSchema.JSONRPCMessage>, Mono<McpSchema.JSONRPCMessage>>> handler =
        new AtomicReference<>(Function.identity());
    private final AtomicReference<Disposable> listening = new AtomicReference<>();
    private final AtomicBoolean closed = new AtomicBoolean();

    MicronautStreamableHttpClientTransport(MicronautMcpHttpExchange exchange,
                                           McpClientHttpConfiguration configuration,
                                           List<McpClientHeadersProvider> headersProviders,
                                           McpJsonMapper jsonMapper) {
        this.exchange = exchange;
        this.configuration = configuration;
        this.headersProviders = headersProviders;
        this.jsonMapper = jsonMapper;
    }

    // The signature is that of the SDK interface
    @SuppressWarnings("java:S4276")
    @Override
    public Mono<Void> connect(Function<Mono<McpSchema.JSONRPCMessage>, Mono<McpSchema.JSONRPCMessage>> handler) {
        this.handler.set(handler);
        return Mono.empty();
    }

    @Override
    public Mono<Void> sendMessage(McpSchema.JSONRPCMessage message) {
        return Mono.deferContextual(reactorContext -> {
            String json;
            try {
                json = jsonMapper.writeValueAsString(message);
            } catch (IOException e) {
                return Mono.error(e);
            }
            // Computed when the client subscribes: the synchronous client computes them on the thread that calls it,
            // before the request may be sent from another thread once the client is initialized
            Map<String, String> headers = TransportHeaders.headers(configuration, headersProviders,
                reactorContext.getOrDefault(McpTransportContext.KEY, McpTransportContext.EMPTY), reactorContext);
            Object requestId = message instanceof McpSchema.JSONRPCRequest request ? request.id() : null;
            boolean initialize = message instanceof McpSchema.JSONRPCRequest request && McpSchema.METHOD_INITIALIZE.equals(request.method());
            Mono<Void> sent = exchange.post(json, headers, initialize)
                .concatMap(response -> receive(response, requestId))
                .then()
                .onErrorMap(McpHttpSessionNotFoundException.class, e -> new McpTransportSessionNotFoundException(e.getSessionId(), e));
            if (message instanceof McpSchema.JSONRPCNotification notification
                && McpSchema.METHOD_NOTIFICATION_INITIALIZED.equals(notification.method())) {
                sent = sent.doOnSuccess(ignored -> listen());
            }
            return sent;
        });
    }

    private Mono<Void> receive(String json, @Nullable Object requestId) {
        McpSchema.JSONRPCMessage message;
        try {
            McpError unrelated = requestId != null && json.contains("\"error\"") ? unrelatedError(json, requestId) : null;
            if (unrelated != null) {
                return Mono.error(unrelated);
            }
            message = McpSchema.deserializeJsonRpcMessage(jsonMapper, json);
        } catch (IOException e) {
            return Mono.error(e);
        }
        if (message instanceof McpSchema.JSONRPCResponse response) {
            if (response.result() instanceof Map<?, ?> result
                && result.get("protocolVersion") instanceof String version
                && result.containsKey("serverInfo")) {
                // The response to initialize: later requests carry the negotiated version
                exchange.setProtocolVersion(version);
            }
        }
        // The client session answers requests of the server, such as sampling, by sending a message itself
        return handler.get().apply(Mono.just(message)).then();
    }

    /**
     * An error the server could not relate to the request, for example about the session, which the client session would
     * discard, and the SDK does not read without id: it fails the request instead.
     */
    private @Nullable McpError unrelatedError(String json, Object requestId) throws IOException {
        Map<String, Object> response = jsonMapper.readValue(json, MAP_TYPE);
        Object error = response.get("error");
        if (error == null || requestId.toString().equals(String.valueOf(response.get("id")))) {
            return null;
        }
        return new McpError(jsonMapper.convertValue(error, McpSchema.JSONRPCResponse.JSONRPCError.class));
    }

    private void listen() {
        if (closed.get()) {
            return;
        }
        Disposable stream = Flux.defer(() -> exchange.listen(exchange.headers()))
            .concatMap(message -> receive(message, null))
            // The server may end the stream at any time
            .repeatWhen(ends -> ends.delayElements(RECONNECT_DELAY).takeWhile(ignored -> !closed.get()))
            .retryWhen(Retry.backoff(Long.MAX_VALUE, RECONNECT_DELAY)
                .maxBackoff(MAX_RECONNECT_DELAY)
                .filter(e -> !closed.get() && !isFinal(e)))
            .onErrorResume(MicronautStreamableHttpClientTransport::isFinal, e -> Flux.empty())
            .subscribe();
        Disposable previous = listening.getAndSet(stream);
        if (previous != null) {
            previous.dispose();
        }
    }

    private static boolean isFinal(Throwable e) {
        // The session ended, the next one opens its own stream, or the server does not offer a stream
        return e instanceof McpHttpSessionNotFoundException
            || e instanceof HttpClientResponseException response && response.getStatus() == HttpStatus.METHOD_NOT_ALLOWED;
    }

    @Override
    public Mono<Void> closeGracefully() {
        return Mono.defer(() -> {
            closed.set(true);
            Disposable stream = listening.getAndSet(null);
            if (stream != null) {
                stream.dispose();
            }
            return exchange.close(exchange.headers());
        });
    }

    @Override
    public <T> T unmarshalFrom(Object data, TypeRef<T> typeRef) {
        return jsonMapper.convertValue(data, typeRef);
    }

    @Override
    public List<String> protocolVersions() {
        return PROTOCOL_VERSIONS;
    }
}
