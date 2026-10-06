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
package io.micronaut.mcp.client.langchain4j.stdio;

import dev.langchain4j.mcp.client.transport.stdio.StdioMcpTransport;
import io.micronaut.context.annotation.EachBean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Prototype;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.Internal;
import io.micronaut.mcp.conf.client.McpClientStdioConfiguration;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Map;

/**
 * Creates a {@link StdioMcpTransport} for each STDIO connection.
 */
@Internal
@Factory
class StdioMcpTransportFactory {
    private static final String LEGACY_NAME = "stdio";

    /**
     * Adapts the deprecated {@value StdioMcpTransportConfiguration#PROPERTY_COMMANDS} property to a STDIO connection
     * named {@value #LEGACY_NAME}.
     *
     * @param configuration The deprecated configuration
     * @return The STDIO connection
     */
    // Bridges the deprecated configuration, until it is removed
    @SuppressWarnings({"removal", "java:S5738"})
    @Named(LEGACY_NAME)
    @Singleton
    @Requires(bean = StdioMcpTransportConfiguration.class)
    McpClientStdioConfiguration legacyStdioConfiguration(StdioMcpTransportConfiguration configuration) {
        return new McpClientStdioConfiguration() {
            @Override
            public @NonNull String getName() {
                return LEGACY_NAME;
            }

            @Override
            public @NonNull List<String> getCommand() {
                return configuration.getCommands();
            }

            @Override
            public @NonNull Map<String, String> getEnvironment() {
                return Map.of();
            }

            @Override
            public boolean isLogEvents() {
                return false;
            }
        };
    }

    @EachBean(McpClientStdioConfiguration.class)
    @Prototype
    StdioMcpTransport.Builder createStdioMcpTransportBuilder(McpClientStdioConfiguration configuration) {
        StdioMcpTransport.Builder builder = new StdioMcpTransport.Builder()
            .command(configuration.getCommand())
            .logEvents(configuration.isLogEvents());
        if (!configuration.getEnvironment().isEmpty()) {
            builder.environment(configuration.getEnvironment());
        }
        return builder;
    }

    // A new transport for each client, which owns and closes it
    @EachBean(StdioMcpTransport.Builder.class)
    @Prototype
    StdioMcpTransport createStdioMcpTransport(StdioMcpTransport.Builder builder) {
        return builder.build();
    }
}
