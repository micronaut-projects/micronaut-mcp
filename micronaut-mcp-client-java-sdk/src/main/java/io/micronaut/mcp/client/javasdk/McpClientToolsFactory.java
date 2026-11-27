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

import io.micronaut.context.BeanContext;
import io.micronaut.context.annotation.EachBean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Primary;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.propagation.PropagatedContext;
import io.micronaut.inject.qualifiers.Qualifiers;
import io.micronaut.mcp.conf.client.McpClientConnectionConfiguration;
import io.micronaut.mcp.conf.client.McpClientHeadersProvider;
import io.micronaut.mcp.conf.client.McpClientHttpConfiguration;
import io.micronaut.mcp.conf.client.McpClientRequestHeaders;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

/**
 * Creates the {@link McpClientTools} of each connection, and the primary one of every connection.
 */
@Factory
@Internal
final class McpClientToolsFactory {
    private static final Logger LOG = LoggerFactory.getLogger(McpClientToolsFactory.class);

    private final BeanContext beanContext;
    private final McpConnectionClients connectionClients;
    private final @Nullable McpClientToolFilter filter;
    private final @Nullable McpClientToolNameMapper nameMapper;
    private final List<McpClientHeadersProvider> headersProviders;

    McpClientToolsFactory(BeanContext beanContext,
                          McpConnectionClients connectionClients,
                          @Nullable McpClientToolFilter filter,
                          @Nullable McpClientToolNameMapper nameMapper,
                          List<McpClientHeadersProvider> headersProviders) {
        this.headersProviders = headersProviders;
        this.beanContext = beanContext;
        this.connectionClients = connectionClients;
        this.filter = filter;
        this.nameMapper = nameMapper;
    }

    @EachBean(McpClientConnectionConfiguration.class)
    @Singleton
    McpClientTools connectionTools(McpClientConnectionConfiguration configuration) {
        // The tools a server lists may depend on the credentials of the caller, so they are cached only when the headers
        // of the connection are the same for every request
        boolean cached = !(configuration instanceof McpClientHttpConfiguration http && McpClientRequestHeaders.isDynamic(http, headersProviders));
        return new ConnectionMcpClientTools(configuration.getName(), beanContext, connectionClients, filter, nameMapper, cached);
    }

    @Primary
    @Singleton
    McpClientTools allTools(List<McpClientConnectionConfiguration> connections) {
        List<String> names = connections.stream().map(McpClientConnectionConfiguration::getName).toList();
        return () -> {
            // Callers may already occupy every thread of Micronaut's blocking executor. Each lookup runs in the context of
            // the caller, such as the HTTP request whose credentials the headers providers forward
            PropagatedContext context = PropagatedContext.getOrEmpty();
            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                List<CompletableFuture<List<McpClientTool>>> tools = names.stream()
                    .map(name -> CompletableFuture.supplyAsync(context.wrap((Supplier<List<McpClientTool>>) () -> toolsOf(name)), executor))
                    .toList();
                return tools.stream().flatMap(t -> t.join().stream()).toList();
            }
        };
    }

    private List<McpClientTool> toolsOf(String name) {
        try {
            return beanContext.getBean(McpClientTools.class, Qualifiers.byName(name)).tools();
        } catch (RuntimeException e) {
            LOG.warn("Failed to list the tools of the MCP connection {}", name, e);
            return List.of();
        }
    }
}
