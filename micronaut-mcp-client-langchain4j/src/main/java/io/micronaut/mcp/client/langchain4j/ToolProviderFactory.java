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
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.EachBean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Parameter;
import io.micronaut.context.annotation.Primary;
import io.micronaut.context.annotation.Prototype;
import io.micronaut.core.annotation.Internal;
import io.micronaut.inject.BeanDefinition;
import io.micronaut.mcp.conf.client.McpClientConnectionConfiguration;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
    McpToolProvider.Builder toolProviderBuilder(List<McpClientConnectionConfiguration> connections,
                                               List<ConnectionMcpClient> connectionClients,
                                               BeanContext beanContext) {
        McpClientConnections.requireUniqueNames(connections);
        Set<String> connectionNames = connectionClients.stream().map(ConnectionMcpClient::key).collect(Collectors.toSet());
        List<McpClient> clients = new ArrayList<>(connectionClients);
        // Inspect definitions first so configured clients are only resolved through their shared lazy wrappers.
        for (BeanDefinition<McpClient> definition : beanContext.getBeanDefinitions(McpClient.class)) {
            if (definition.getBeanName().filter(connectionNames::contains).isEmpty()) {
                clients.add(beanContext.getBean(definition));
            }
        }
        return builder(clients);
    }

    @Singleton
    @Primary
    McpToolProvider toolProvider(@Primary McpToolProvider.Builder builder) {
        return builder.build();
    }

    @EachBean(McpClientConnectionConfiguration.class)
    @Prototype
    McpToolProvider.Builder clientToolProviderBuilder(@Parameter ConnectionMcpClient client) {
        return builder(List.of(client));
    }

    @EachBean(McpClientConnectionConfiguration.class)
    @Singleton
    McpToolProvider clientToolProvider(@Parameter McpToolProvider.Builder builder) {
        return builder.build();
    }

    /**
     * The client of a connection, shared by the tool providers and the health indicator. It resolves the client bean
     * when the tools are provided, so that a server that cannot be reached is skipped instead of failing the creation
     * of the tool providers.
     *
     * @param connection The connection
     * @param beanContext The bean context
     * @return The client of the connection
     */
    @EachBean(McpClientConnectionConfiguration.class)
    @Singleton
    @Bean(typed = ConnectionMcpClient.class)
    ConnectionMcpClient connectionClient(McpClientConnectionConfiguration connection, BeanContext beanContext) {
        return new ConnectionMcpClient(connection.getName(), beanContext);
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
