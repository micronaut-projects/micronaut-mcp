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
package io.micronaut.mcp.server;

import io.micronaut.context.BeanContext;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.context.event.StartupEvent;
import io.micronaut.core.annotation.Internal;
import io.micronaut.mcp.server.conf.McpServerConfiguration;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpStatelessAsyncServer;
import io.modelcontextprotocol.server.McpStatelessSyncServer;
import io.modelcontextprotocol.server.McpSyncServer;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Singleton;

import java.io.Closeable;

@Requires(beans = McpServerConfiguration.class)
@Internal
@Singleton
final class McpServerBootstrap implements ApplicationEventListener<StartupEvent>, AutoCloseable {
    private final BeanContext beanContext;
    private final McpServerConfiguration mcpServerConfiguration;

    private Closeable mcpServer;

    McpServerBootstrap(BeanContext beanContext,
                       McpServerConfiguration mcpServerConfiguration) {
        this.beanContext = beanContext;
        this.mcpServerConfiguration = mcpServerConfiguration;
    }

    @Override
    public void onApplicationEvent(StartupEvent event) {
        this.mcpServer = switch (mcpServerConfiguration.getType()) {
            case SYNC -> beanContext.getBean(McpSyncServer.class)::close;
            case ASYNC -> beanContext.getBean(McpAsyncServer.class)::close;
            case STATELESS_ASYNC -> beanContext.getBean(McpStatelessAsyncServer.class)::close;
            case STATELESS_SYNC -> beanContext.getBean(McpStatelessSyncServer.class)::close;
        };
    }

    @PreDestroy
    @Override
    public void close() throws Exception {
        mcpServer.close();
    }
}
