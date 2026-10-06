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

import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.McpClient;
import io.micronaut.context.BeanContext;
import io.micronaut.context.annotation.EachBean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Primary;
import io.micronaut.context.annotation.Prototype;
import io.micronaut.core.annotation.Internal;
import io.micronaut.mcp.conf.client.McpClientConnectionConfiguration;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Creates an {@link McpToolProvider} of the tools of every MCP client, and one per client, qualified by the name of its connection.
 */
@Internal
@Factory
final class ToolProviderFactory {
    private final @Nullable McpToolFilter filter;
    private final @Nullable McpToolNameMapper nameMapper;

    ToolProviderFactory(@Nullable McpToolFilter filter, @Nullable McpToolNameMapper nameMapper) {
        this.filter = filter;
        this.nameMapper = nameMapper;
    }

    @Prototype
    @Primary
    McpToolProvider.Builder toolProviderBuilder(List<McpClientConnectionConfiguration> connections, BeanContext beanContext) {
        // The clients are resolved when the tools are provided, so that a server that cannot be reached is skipped
        // instead of failing the creation of the tool provider
        List<McpClient> clients = connections.stream()
            .<McpClient>map(connection -> new ConnectionMcpClient(connection.getName(), beanContext))
            .toList();
        return builder(clients);
    }

    @Singleton
    @Primary
    McpToolProvider toolProvider(@Primary McpToolProvider.Builder builder) {
        return builder.build();
    }

    @EachBean(McpClient.class)
    @Singleton
    McpToolProvider clientToolProvider(McpClient client) {
        return builder(List.of(client)).build();
    }

    private McpToolProvider.Builder builder(List<McpClient> clients) {
        McpToolProvider.Builder builder = McpToolProvider.builder().mcpClients(clients);
        if (filter != null) {
            builder.filter(filter);
        }
        if (nameMapper != null) {
            builder.toolNameMapper(nameMapper);
        }
        return builder;
    }
}
