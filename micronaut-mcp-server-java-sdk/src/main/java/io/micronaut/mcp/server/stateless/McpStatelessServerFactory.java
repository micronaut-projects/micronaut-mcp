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
package io.micronaut.mcp.server.stateless;

import io.micronaut.context.annotation.Factory;
import org.jspecify.annotations.Nullable;
import io.modelcontextprotocol.server.McpStatelessServerHandler;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpStatelessServerTransport;
import jakarta.inject.Singleton;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * A factory for {@link McpStatelessServerHandler} and {@link McpStatelessServerTransport}.
 */
@Factory
final class McpStatelessServerFactory {

    @Nullable
    private McpStatelessServerHandler serverHandler;

    @Singleton
    McpStatelessServerTransport serverHandler() {
        return new McpStatelessServerTransport() {
            @Override
            public void setMcpHandler(McpStatelessServerHandler mcpHandler) {
                serverHandler = mcpHandler;
            }

            @Override
            public Mono<Void> closeGracefully() {
                return Mono.empty();
            }
        };
    }

    @Singleton
    McpStatelessServerHandler serverTransport() {
        return new McpStatelessServerHandler() {
            @Override
            public Mono<McpSchema.JSONRPCResponse> handleRequest(McpTransportContext transportContext, McpSchema.JSONRPCRequest request) {
                checkServerHandler();
                return serverHandler.handleRequest(transportContext, request);
            }

            @Override
            public Mono<Void> handleNotification(McpTransportContext transportContext, McpSchema.JSONRPCNotification notification) {
                checkServerHandler();
                return serverHandler.handleNotification(transportContext, notification);
            }

            private void checkServerHandler() {
                Objects.requireNonNull(serverHandler, "McpStatelessServerHandler is not set");
            }
        };
    }

}
