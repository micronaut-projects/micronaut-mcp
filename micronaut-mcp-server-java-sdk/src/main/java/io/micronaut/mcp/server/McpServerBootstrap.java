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
import io.micronaut.context.event.ShutdownEvent;
import io.micronaut.context.event.StartupEvent;
import io.micronaut.core.annotation.Internal;
import io.micronaut.mcp.conf.server.McpServerConfiguration;
import io.micronaut.runtime.event.annotation.EventListener;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpStatelessAsyncServer;
import io.modelcontextprotocol.server.McpStatelessSyncServer;
import io.modelcontextprotocol.server.McpSyncServer;
import jakarta.inject.Singleton;

@Requires(beans = McpServerConfiguration.class)
@Internal
@Singleton
final class McpServerBootstrap {
    private final BeanContext beanContext;
    private final McpServerConfiguration mcpServerConfiguration;

    private Object mcpServer;

    McpServerBootstrap(BeanContext beanContext,
                       McpServerConfiguration mcpServerConfiguration) {
        this.beanContext = beanContext;
        this.mcpServerConfiguration = mcpServerConfiguration;
    }

    @EventListener
    public void onStartupEvent(StartupEvent startupEvent) {
        if (this.mcpServer == null) {
            this.mcpServer = beanContext.getBean(beanTypeForConfiguration(mcpServerConfiguration));
        }
    }

    @EventListener
    public void onShutDownEvent(ShutdownEvent shutdownEvent) {
        if (mcpServer instanceof McpAsyncServer server) {
            server.close();
        } else if (mcpServer instanceof McpSyncServer server) {
            server.close();
        } else if (mcpServer instanceof McpStatelessAsyncServer server) {
            server.close();
        } else if (mcpServer instanceof McpStatelessSyncServer server) {
            server.close();
        }
    }

    private static Class<?> beanTypeForConfiguration(McpServerConfiguration mcpServerConfiguration) {
        return switch (mcpServerConfiguration.getTransport()) {
            case STDIO -> mcpServerConfiguration.isReactive() ? McpAsyncServer.class : McpSyncServer.class;
            case HTTP -> mcpServerConfiguration.isReactive() ? McpStatelessAsyncServer.class : McpStatelessSyncServer.class;
        };
    }
}
