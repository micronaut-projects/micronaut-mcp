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
package io.micronaut.mcp.server.sdk.stateless;

import io.micronaut.context.BeanContext;
import io.micronaut.core.annotation.Internal;
import io.modelcontextprotocol.server.McpStatelessServerHandler;
import io.modelcontextprotocol.spec.McpStatelessServerTransport;
import jakarta.inject.Singleton;
import reactor.core.publisher.Mono;

/**
 * Implementation of {@link McpStatelessServerTransport} which registers a singleton of type {@link McpStatelessServerHandler} when the method {@link McpStatelessServerTransport#setMcpHandler(McpStatelessServerHandler)} is invoked.
 */
@Internal
@Singleton
class MicronautMcpStatelessServerTransport implements McpStatelessServerTransport {
    private final BeanContext beanContext;

    MicronautMcpStatelessServerTransport(BeanContext beanContext) {
        this.beanContext = beanContext;
    }

    @Override
    public void setMcpHandler(McpStatelessServerHandler mcpHandler) {
        beanContext.registerSingleton(McpStatelessServerHandler.class, mcpHandler);
    }

    @Override
    public Mono<Void> closeGracefully() {
        return Mono.empty();
    }
}
