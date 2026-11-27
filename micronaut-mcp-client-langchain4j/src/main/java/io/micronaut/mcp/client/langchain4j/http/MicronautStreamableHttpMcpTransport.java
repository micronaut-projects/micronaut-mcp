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
package io.micronaut.mcp.client.langchain4j.http;

import com.fasterxml.jackson.databind.JsonNode;
import dev.langchain4j.mcp.client.McpCallContext;
import dev.langchain4j.mcp.client.McpException;
import dev.langchain4j.mcp.client.transport.McpHeaderEncoding;
import dev.langchain4j.mcp.client.transport.McpJson;
import dev.langchain4j.mcp.client.transport.McpOperationHandler;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.protocol.McpClientMessage;
import dev.langchain4j.mcp.protocol.McpInitializationNotification;
import dev.langchain4j.mcp.protocol.McpInitializeRequest;
import io.micronaut.core.annotation.Internal;
import io.micronaut.http.client.exceptions.HttpClientException;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.http.client.exceptions.ReadTimeoutException;
import io.micronaut.mcp.client.http.McpHttpSessionNotFoundException;
import io.micronaut.mcp.client.http.MicronautMcpHttpExchange;
import org.jspecify.annotations.Nullable;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A LangChain4j MCP transport over Streamable HTTP, on the Micronaut HTTP client. As the LangChain4j HTTP transport, it
 * initializes a new session when the server no longer knows the session, and does not listen to a stream of the messages
 * the server sends on its own.
 */
@Internal
final class MicronautStreamableHttpMcpTransport implements McpTransport {
    private static final String MCP_METHOD = "Mcp-Method";
    private static final String MCP_NAME = "Mcp-Name";
    private static final String ERROR = "error";
    private static final Duration CLOSE_TIMEOUT = Duration.ofSeconds(10);

    private final MicronautMcpHttpExchange exchange;
    private final AtomicReference<McpOperationHandler> operationHandler = new AtomicReference<>();
    private final AtomicReference<Runnable> onFailure = new AtomicReference<>();
    private final AtomicReference<String> protocolVersion = new AtomicReference<>();
    private final AtomicBoolean modernProtocol = new AtomicBoolean();
    private final AtomicReference<String> negotiatedVersion = new AtomicReference<>();
    private @Nullable Session session;

    MicronautStreamableHttpMcpTransport(MicronautMcpHttpExchange exchange) {
        this.exchange = exchange;
    }

    @Override
    public void start(McpOperationHandler messageHandler) {
        operationHandler.set(messageHandler);
    }

    @Override
    public CompletableFuture<String> sendInitializeRequest(McpInitializeRequest request) {
        return initialize(request, exchange.headers()).ready();
    }

    /**
     * Initializes a session with the given headers, such as those of the caller whose request found the session
     * expired, which the response scheduler could not compute again.
     */
    private synchronized Session initialize(McpInitializeRequest request, Map<String, String> headers) {
        negotiatedVersion.set(null);
        updateProtocolVersion();
        McpCallContext context = new McpCallContext(null, request);
        CompletableFuture<String> ready = execute(context, post(context, headers))
            .thenCompose(response -> {
                JsonNode message = McpJson.parse(response);
                if (message.has(ERROR)) {
                    throw rpcError(message.get(ERROR));
                }
                JsonNode version = message.path("result").path("protocolVersion");
                if (!version.isTextual()) {
                    throw new IllegalStateException("The MCP initialize result is missing its protocol version");
                }
                negotiatedVersion.set(version.textValue());
                updateProtocolVersion();
                McpCallContext initialized = new McpCallContext(null, new McpInitializationNotification());
                return execute(initialized, post(initialized, headers)).thenApply(ignored -> response);
            });
        session = new Session(request, ready);
        return session;
    }

    @Override
    public CompletableFuture<String> sendRequest(McpClientMessage request) {
        return sendRequest(new McpCallContext(null, request));
    }

    @Override
    public CompletableFuture<String> sendRequest(McpCallContext context) {
        // Compute request headers on the calling thread, including those needed for a later renewal.
        Map<String, String> headers = exchange.headers();
        return send(context, headers);
    }

    private synchronized CompletableFuture<String> send(McpCallContext context, Map<String, String> headers) {
        Flux<String> messages = post(context, headers);
        Session current = session;
        if (current == null || modernProtocol.get()) {
            return execute(context, messages.doOnError(McpHttpSessionNotFoundException.class, exchange::invalidateSession));
        }
        Session active = current.ready().isCompletedExceptionally() ? initialize(current.request(), headers) : current;
        Flux<String> pending = afterHandshake(active, messages.onErrorResume(McpHttpSessionNotFoundException.class, expired -> {
            Session replacement = renew(active, expired, headers);
            return afterHandshake(replacement, messages.doOnError(McpHttpSessionNotFoundException.class,
                error -> markExpired(replacement, error)));
        }));
        return execute(context, pending);
    }

    private static Flux<String> afterHandshake(Session session, Flux<String> messages) {
        // Cancelling the operation cancels its pending replay, while other callers can still use the handshake.
        return Mono.fromFuture(session.ready(), true).thenMany(messages);
    }

    private synchronized Session renew(Session expired, McpHttpSessionNotFoundException error, Map<String, String> headers) {
        Session current = session;
        if (current != null && current != expired) {
            return current;
        }
        exchange.invalidateSession(error);
        return initialize(expired.request(), headers);
    }

    private synchronized void markExpired(Session expired, McpHttpSessionNotFoundException error) {
        if (session == expired && exchange.invalidateSession(error)) {
            // This request used its retry. The next caller can start another session with its own headers.
            session = new Session(expired.request(), CompletableFuture.failedFuture(error));
        }
    }

    @Override
    public void sendMessage(McpClientMessage request) {
        sendRequest(request);
    }

    @Override
    public void sendMessage(McpCallContext context) {
        sendRequest(context);
    }

    @Override
    public void setModernProtocol(boolean modernProtocol) {
        this.modernProtocol.set(modernProtocol);
        updateProtocolVersion();
    }

    @Override
    public void setProtocolVersion(String protocolVersion) {
        this.protocolVersion.set(protocolVersion);
        updateProtocolVersion();
    }

    private void updateProtocolVersion() {
        // Protocol detection supplies the modern version. A legacy handshake obtains its version from the server.
        boolean modern = modernProtocol.get();
        exchange.setProtocolVersion(modern ? protocolVersion.get() : negotiatedVersion.get());
        exchange.setSessionless(modern);
    }

    private Flux<String> post(McpCallContext context, Map<String, String> headers) {
        String json = McpJson.serialize(context.message());
        Long id = context.message().getId();
        boolean initialize = context.message() instanceof McpInitializeRequest;
        return exchange.post(json, requestHeaders(context, json, headers), initialize, id != null ? message -> isResponseTo(message, id) : null)
            .doOnError(error -> {
                Runnable failure = onFailure.get();
                if (failure != null && isConnectionLost(error)) {
                    failure.run();
                }
            });
    }

    private CompletableFuture<String> execute(McpCallContext context, Flux<String> messages) {
        McpOperationHandler handler = operationHandler.get();
        if (handler == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("The transport is not started"));
        }
        Long id = context.message().getId();
        CompletableFuture<String> future = new CompletableFuture<>();
        if (id != null) {
            // The operation handler completes the future when it receives the response with this id
            handler.expectResponse(id, future);
        }
        Disposable exchanged = messages.subscribe(
            message -> receive(handler, message, id, future),
            future::completeExceptionally,
            () -> {
                if (id == null) {
                    future.complete(null);
                } else if (!future.isDone()) {
                    // The response ended without answering the request
                    future.completeExceptionally(new IllegalStateException("The MCP server did not answer the request " + id));
                }
            });
        // When the client gives up on the request, for example after its timeout, the exchange is cancelled
        future.whenComplete((response, error) -> {
            if (error != null) {
                exchanged.dispose();
            }
        });
        return future;
    }

    private Map<String, String> requestHeaders(McpCallContext context, String json, Map<String, String> headers) {
        if (!modernProtocol.get()) {
            return headers;
        }
        // The 2026-07-28 protocol names the method, and the tool, prompt or resource, in headers, as the LangChain4j HTTP
        // transport
        Map<String, String> requestHeaders = new LinkedHashMap<>(headers);
        JsonNode message = McpJson.parse(json);
        String method = message.path("method").asText(null);
        if (method != null) {
            requestHeaders.put(MCP_METHOD, method);
            JsonNode params = message.path("params");
            String name = switch (method) {
                case "tools/call", "prompts/get" -> params.path("name").asText(null);
                case "resources/read" -> params.path("uri").asText(null);
                default -> null;
            };
            if (name != null) {
                requestHeaders.put(MCP_NAME, McpHeaderEncoding.encode(name));
            }
        }
        if (context.mcpParamHeaders() != null) {
            requestHeaders.putAll(context.mcpParamHeaders());
        }
        return requestHeaders;
    }

    /**
     * @return Whether the message is the response to the request, which ends the resumption of its stream
     */
    private static boolean isResponseTo(String message, long id) {
        if (!message.contains("\"result\"") && !message.contains("\"error\"")) {
            return false;
        }
        JsonNode node = McpJson.parse(message);
        JsonNode responseId = node.get("id");
        return !node.has("method") && responseId != null && responseId.isNumber() && responseId.asLong() == id;
    }

    private static void receive(McpOperationHandler handler, String message, @Nullable Long id, CompletableFuture<String> future) {
        if (id != null && message.contains("\"error\"")) {
            JsonNode node = McpJson.parse(message);
            JsonNode responseId = node.get("id");
            if (node.has(ERROR) && (responseId == null || !responseId.isNumber() || responseId.asLong() != id)) {
                // An error the server could not relate to the request, for example about the session, which the
                // operation handler would discard: it fails the request instead
                future.completeExceptionally(rpcError(node.get(ERROR)));
                return;
            }
        }
        handler.onMessage(message);
    }

    private static McpException rpcError(JsonNode error) {
        int code = error.path("code").asInt();
        String text = error.path("message").asText();
        JsonNode data = error.get("data");
        return data == null ? new McpException(code, text) : McpException.withErrorData(code, text, data.toString());
    }

    private static boolean isConnectionLost(Throwable error) {
        return error instanceof HttpClientException
            && !(error instanceof HttpClientResponseException)
            && !(error instanceof ReadTimeoutException);
    }

    @Override
    public void checkHealth() {
        // the client pings the server
    }

    @Override
    public void onFailure(Runnable actionOnFailure) {
        onFailure.set(actionOnFailure);
    }

    @Override
    public void close() {
        Mono<Void> close = exchange.close(exchange.headers());
        if (Schedulers.isInNonBlockingThread()) {
            // Never block an event loop
            close.subscribe();
        } else {
            close.block(CLOSE_TIMEOUT);
        }
    }

    private record Session(McpInitializeRequest request, CompletableFuture<String> ready) {
    }
}
