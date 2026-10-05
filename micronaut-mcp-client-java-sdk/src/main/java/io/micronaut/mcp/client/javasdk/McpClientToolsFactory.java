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
import io.modelcontextprotocol.client.McpSyncClient;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Creates the {@link McpClientTools} of each connection, and the primary one of every connection.
 */
@Factory
@Internal
final class McpClientToolsFactory {
    private final BeanContext beanContext;
    private final @Nullable McpClientToolFilter filter;
    private final @Nullable McpClientToolNameMapper nameMapper;

    McpClientToolsFactory(BeanContext beanContext, @Nullable McpClientToolFilter filter, @Nullable McpClientToolNameMapper nameMapper) {
        this.beanContext = beanContext;
        this.filter = filter;
        this.nameMapper = nameMapper;
    }

    @EachBean(McpClientConnectionConfiguration.class)
    @Singleton
    McpClientTools connectionTools(McpClientConnectionConfiguration configuration) {
        McpSyncClient client = beanContext.getBean(McpSyncClient.class, Qualifiers.byName(configuration.getName()));
        return new ConnectionMcpClientTools(configuration.getName(), client, filter, nameMapper);
    }

    @Primary
    @Singleton
    McpClientTools allTools(List<McpClientConnectionConfiguration> connections) {
        List<McpClientTools> tools = connections.stream()
            .map(c -> beanContext.getBean(McpClientTools.class, Qualifiers.byName(c.getName())))
            .toList();
        return () -> tools.stream().flatMap(t -> t.tools().stream()).toList();
    }
}
