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
import io.micronaut.inject.qualifiers.Qualifiers;
import io.micronaut.mcp.conf.client.McpClientConnectionConfiguration;
import io.micronaut.scheduling.TaskExecutors;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

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

    McpClientToolsFactory(BeanContext beanContext,
                          McpConnectionClients connectionClients,
                          @Nullable McpClientToolFilter filter,
                          @Nullable McpClientToolNameMapper nameMapper) {
        this.beanContext = beanContext;
        this.connectionClients = connectionClients;
        this.filter = filter;
        this.nameMapper = nameMapper;
    }

    @EachBean(McpClientConnectionConfiguration.class)
    @Singleton
    McpClientTools connectionTools(McpClientConnectionConfiguration configuration) {
        return new ConnectionMcpClientTools(configuration.getName(), beanContext, connectionClients, filter, nameMapper);
    }

    @Primary
    @Singleton
    McpClientTools allTools(List<McpClientConnectionConfiguration> connections,
                            @Named(TaskExecutors.BLOCKING) ExecutorService blockingExecutor) {
        List<String> names = connections.stream().map(McpClientConnectionConfiguration::getName).toList();
        return () -> {
            // The servers are asked concurrently, and those that cannot be reached are skipped
            List<CompletableFuture<List<McpClientTool>>> tools = names.stream()
                .map(name -> CompletableFuture.supplyAsync(() -> toolsOf(name), blockingExecutor))
                .toList();
            return tools.stream().flatMap(t -> t.join().stream()).toList();
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
