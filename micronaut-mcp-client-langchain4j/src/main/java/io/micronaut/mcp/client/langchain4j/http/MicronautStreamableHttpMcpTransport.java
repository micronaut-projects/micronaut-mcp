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
    private static final Duration CLOSE_TIMEOUT = Duration.ofSeconds(10);

    private final MicronautMcpHttpExchange exchange;
    private final AtomicReference<McpOperationHandler> operationHandler = new AtomicReference<>();
    private final AtomicReference<Runnable> onFailure = new AtomicReference<>();
    private final AtomicReference<McpInitializeRequest> initializeRequest = new AtomicReference<>();
    private final AtomicReference<String> protocolVersion = new AtomicReference<>();
    private final AtomicBoolean modernProtocol = new AtomicBoolean();

    MicronautStreamableHttpMcpTransport(MicronautMcpHttpExchange exchange) {
        this.exchange = exchange;
    }

    @Override
    public void start(McpOperationHandler messageHandler) {
        operationHandler.set(messageHandler);
    }

    @Override
    public CompletableFuture<String> sendInitializeRequest(McpInitializeRequest request) {
        initializeRequest.set(request);
        Map<String, String> headers = exchange.headers();
        return execute(new McpCallContext(null, request), headers, true)
            .thenCompose(response -> execute(new McpCallContext(null, new McpInitializationNotification()), headers, true)
                .thenApply(ignored -> response));
    }

    @Override
    public CompletableFuture<String> sendRequest(McpClientMessage request) {
        return sendRequest(new McpCallContext(null, request));
    }

    @Override
    public CompletableFuture<String> sendRequest(McpCallContext context) {
        // The headers are computed on the thread that calls the client
        return execute(context, exchange.headers(), false);
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
        // As the LangChain4j HTTP transport: the header is only sent with the 2026-07-28 protocol, whose detection
        // falls back to the initialization of earlier versions, and which has no sessions
        boolean modern = modernProtocol.get();
        exchange.setProtocolVersion(modern ? protocolVersion.get() : null);
        exchange.setSessionless(modern);
    }

    private CompletableFuture<String> execute(McpCallContext context, Map<String, String> headers, boolean retried) {
        McpOperationHandler handler = operationHandler.get();
        if (handler == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("The transport is not started"));
        }
        String json = McpJson.serialize(context.message());
        Long id = context.message().getId();
        CompletableFuture<String> future = new CompletableFuture<>();
        if (id != null) {
            // The operation handler completes the future when it receives the response with this id
            handler.expectResponse(id, future);
        }
        boolean initialize = context.message() instanceof McpInitializeRequest;
        Disposable exchanged = exchange.post(json, requestHeaders(context, json, headers), initialize).subscribe(
            message -> receive(handler, message, id, future),
            error -> failed(context, headers, retried, error, future),
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

    private static void receive(McpOperationHandler handler, String message, @Nullable Long id, CompletableFuture<String> future) {
        if (id != null && message.contains("\"error\"")) {
            JsonNode node = McpJson.parse(message);
            JsonNode responseId = node.get("id");
            if (node.has("error") && (responseId == null || !responseId.isNumber() || responseId.asLong() != id)) {
                // An error the server could not relate to the request, for example about the session, which the
                // operation handler would discard: it fails the request instead
                JsonNode error = node.get("error");
                future.completeExceptionally(new McpException(error.path("code").asInt(), error.path("message").asText(), error.get("data")));
                return;
            }
        }
        handler.onMessage(message);
    }

    private void failed(McpCallContext context, Map<String, String> headers, boolean retried, Throwable error, CompletableFuture<String> future) {
        McpInitializeRequest initialize = initializeRequest.get();
        if (error instanceof McpHttpSessionNotFoundException && !retried && initialize != null) {
            // As the LangChain4j HTTP transport: initialize a new session, then send the request again once
            sendInitializeRequest(initialize)
                .thenCompose(ignored -> execute(context, headers, true))
                .whenComplete((response, retryError) -> {
                    if (retryError != null) {
                        future.completeExceptionally(retryError);
                    } else {
                        future.complete(response);
                    }
                });
            return;
        }
        future.completeExceptionally(error);
        Runnable failure = onFailure.get();
        if (failure != null && isConnectionLost(error)) {
            // Only when the server cannot be reached, not when it fails a request; called off the event loop
            failure.run();
        }
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
}
