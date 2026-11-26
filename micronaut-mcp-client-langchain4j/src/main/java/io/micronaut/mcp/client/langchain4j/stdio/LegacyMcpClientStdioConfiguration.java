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

import io.micronaut.core.annotation.Internal;
import io.micronaut.mcp.conf.client.McpClientStdioConfiguration;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Map;

/**
 * The STDIO connection named {@value #NAME} that the deprecated {@value #PROPERTY} property declares when the deprecated
 * {@value #TRANSPORT} property is {@value #TRANSPORT_STDIO}.
 */
@Internal
public final class LegacyMcpClientStdioConfiguration implements McpClientStdioConfiguration {
    /**
     * The name of the connection.
     */
    public static final String NAME = "stdio";
    /**
     * The deprecated property that declares the connection.
     */
    @SuppressWarnings("removal")
    public static final String PROPERTY = StdioMcpTransportConfiguration.PROPERTY_COMMANDS;
    /**
     * The deprecated property that must be {@value #TRANSPORT_STDIO} for {@value #PROPERTY} to declare the connection.
     */
    public static final String TRANSPORT = "micronaut.mcp.client.transport";
    /**
     * The value of {@value #TRANSPORT} that enables the connection.
     */
    public static final String TRANSPORT_STDIO = "STDIO";

    private final List<String> command;

    /**
     * @param command The command that starts the server, and its arguments
     */
    LegacyMcpClientStdioConfiguration(List<String> command) {
        this.command = command;
    }

    @Override
    public @NonNull String getName() {
        return NAME;
    }

    @Override
    public @NonNull List<String> getCommand() {
        return command;
    }

    @Override
    public @NonNull Map<String, String> getEnvironment() {
        return Map.of();
    }

    @Override
    public boolean isLogEvents() {
        return false;
    }
}
