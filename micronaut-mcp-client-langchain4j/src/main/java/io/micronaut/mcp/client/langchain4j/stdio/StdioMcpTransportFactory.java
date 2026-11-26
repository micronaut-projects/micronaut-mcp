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


/**
 * Creates a {@link StdioMcpTransport} for each STDIO connection.
 */
@Internal
@Factory
class StdioMcpTransportFactory {
    /**
     * Adapts the deprecated {@value StdioMcpTransportConfiguration#PROPERTY_COMMANDS} property to a STDIO connection
     * named {@value LegacyMcpClientStdioConfiguration#NAME}, only when the deprecated
     * {@value LegacyMcpClientStdioConfiguration#TRANSPORT} property is {@code STDIO}, as before named connections, so
     * that a leftover value does not start a process after an upgrade.
     *
     * @param configuration The deprecated configuration
     * @return The STDIO connection
     */
    // Bridges the deprecated configuration, until it is removed
    @SuppressWarnings({"removal", "java:S5738"})
    @Named(LegacyMcpClientStdioConfiguration.NAME)
    @Singleton
    @Requires(bean = StdioMcpTransportConfiguration.class)
    @Requires(property = LegacyMcpClientStdioConfiguration.TRANSPORT, value = LegacyMcpClientStdioConfiguration.TRANSPORT_STDIO)
    McpClientStdioConfiguration legacyStdioConfiguration(StdioMcpTransportConfiguration configuration) {
        return new LegacyMcpClientStdioConfiguration(configuration.getCommands());
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
