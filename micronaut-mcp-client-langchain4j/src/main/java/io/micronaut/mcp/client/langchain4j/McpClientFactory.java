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
package io.micronaut.mcp.client.langchain4j;

import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.McpClientListener;
import dev.langchain4j.mcp.client.logging.McpLogMessageHandler;
import dev.langchain4j.mcp.client.transport.McpTransport;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.EachBean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Parameter;
import io.micronaut.context.annotation.Prototype;
import io.micronaut.core.annotation.Internal;
import io.micronaut.mcp.conf.client.McpClientConnectionConfiguration;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;

/**
 * Creates an {@link McpClient} for each connection, over the transport of the connection.
 */
@Internal
@Factory
final class McpClientFactory {
    private static final Logger LOG = LoggerFactory.getLogger(McpClientFactory.class);

    @EachBean(McpClientConnectionConfiguration.class)
    @Prototype
    DefaultMcpClient.Builder crateMcpClientBuilder(McpClientConnectionConfiguration configuration,
                                                   List<McpClientListener> listeners,
                                                   @Nullable McpLogMessageHandler logMessageHandler) {
        DefaultMcpClient.Builder builder = new DefaultMcpClient.Builder()
            // The key identifies the client, for example in tool name mappers and filters
            .key(configuration.getName())
            .autoHealthCheck(configuration.isAutoHealthCheck())
            .addListeners(listeners);
        Duration initializationTimeout = configuration.getInitializationTimeout();
        if (initializationTimeout != null) {
            builder.initializationTimeout(initializationTimeout);
        }
        Duration requestTimeout = configuration.getRequestTimeout();
        if (requestTimeout != null) {
            builder.toolExecutionTimeout(requestTimeout)
                .resourcesTimeout(requestTimeout)
                .promptsTimeout(requestTimeout)
                .pingTimeout(requestTimeout);
        }
        if (logMessageHandler != null) {
            builder.logHandler(logMessageHandler);
        }
        return builder;
    }

    @EachBean(McpClientConnectionConfiguration.class)
    @Bean(preDestroy = "close")
    @Singleton
    McpClient createMcpClient(McpClientConnectionConfiguration configuration,
                              @Parameter DefaultMcpClient.Builder builder,
                              @Parameter McpTransport transport) {
        // The client owns its transport, which it closes. The builder and the transport are prototypes, so each attempt
        // to create the client gets a new transport, and a failed attempt must close it so that it does not leave a
        // server process running
        try {
            return builder.transport(transport).build();
        } catch (RuntimeException e) {
            close(transport, configuration);
            throw e;
        }
    }

    private static void close(McpTransport transport, McpClientConnectionConfiguration configuration) {
        try {
            transport.close();
        } catch (Exception e) {
            LOG.warn("Failed to close the transport of the MCP connection {}", configuration.getName(), e);
        }
    }
}
