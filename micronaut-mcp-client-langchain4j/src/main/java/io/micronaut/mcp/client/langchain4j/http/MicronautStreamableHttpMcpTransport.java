/*
 * Copyright 2017-2025 original authors
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

import dev.langchain4j.mcp.client.McpCallContext;
import dev.langchain4j.mcp.client.transport.McpJson;
import dev.langchain4j.mcp.client.transport.McpOperationHandler;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.protocol.McpClientMessage;
import dev.langchain4j.mcp.protocol.McpInitializationNotification;
import dev.langchain4j.mcp.protocol.McpInitializeRequest;
import io.micronaut.core.annotation.Internal;
import io.micronaut.mcp.client.http.MicronautMcpHttpExchange;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * A LangChain4j MCP transport over Streamable HTTP, on the Micronaut HTTP client.
 */
@Internal
final class MicronautStreamableHttpMcpTransport implements McpTransport {
    private final MicronautMcpHttpExchange exchange;
    private volatile @Nullable McpOperationHandler operationHandler;
    private volatile @Nullable Runnable onFailure;
    private volatile boolean modernProtocol;
    private volatile @Nullable String protocolVersion;

    MicronautStreamableHttpMcpTransport(MicronautMcpHttpExchange exchange) {
        this.exchange = exchange;
    }

    @Override
    public void start(McpOperationHandler messageHandler) {
        this.operationHandler = messageHandler;
    }

    @Override
    public CompletableFuture<String> sendInitializeRequest(McpInitializeRequest request) {
        return execute(new McpCallContext(null, request))
            .thenCompose(response -> execute(new McpCallContext(null, new McpInitializationNotification()))
                .thenApply(ignored -> response));
    }

    @Override
    public CompletableFuture<String> sendRequest(McpClientMessage request) {
        return execute(new McpCallContext(null, request));
    }

    @Override
    public CompletableFuture<String> sendRequest(McpCallContext context) {
        return execute(context);
    }

    @Override
    public void sendMessage(McpClientMessage request) {
        execute(new McpCallContext(null, request));
    }

    @Override
    public void sendMessage(McpCallContext context) {
        execute(context);
    }

    @Override
    public void setModernProtocol(boolean modernProtocol) {
        this.modernProtocol = modernProtocol;
        updateProtocolVersion();
    }

    @Override
    public void setProtocolVersion(String protocolVersion) {
        this.protocolVersion = protocolVersion;
        updateProtocolVersion();
    }

    private void updateProtocolVersion() {
        // As the LangChain4j HTTP transport: the header is only sent with the 2026-07-28 protocol, whose detection
        // falls back to the initialization of earlier versions
        exchange.setProtocolVersion(modernProtocol ? protocolVersion : null);
    }

    private CompletableFuture<String> execute(McpCallContext context) {
        McpOperationHandler handler = operationHandler;
        if (handler == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("The transport is not started"));
        }
        Long id = context.message().getId();
        CompletableFuture<String> future = new CompletableFuture<>();
        if (id != null) {
            // The operation handler completes the future when it receives the response with this id
            handler.expectResponse(id, future);
        }
        exchange.post(McpJson.serialize(context.message())).subscribe(
            handler::onMessage,
            error -> {
                future.completeExceptionally(error);
                Runnable failure = onFailure;
                if (failure != null) {
                    failure.run();
                }
            },
            () -> {
                if (id == null) {
                    future.complete(null);
                } else if (!future.isDone()) {
                    // The response ended without answering the request, for example an error the server could not
                    // relate to it
                    future.completeExceptionally(new IllegalStateException("The MCP server did not answer the request " + id));
                }
            });
        return future;
    }

    @Override
    public void checkHealth() {
        // the client pings the server
    }

    @Override
    public void onFailure(Runnable actionOnFailure) {
        this.onFailure = actionOnFailure;
    }

    @Override
    public void close() {
        exchange.close().block();
    }
}
