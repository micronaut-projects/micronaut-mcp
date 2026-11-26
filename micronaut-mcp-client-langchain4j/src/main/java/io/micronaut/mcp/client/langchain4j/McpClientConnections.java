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

import io.micronaut.context.exceptions.ConfigurationException;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.naming.Named;
import io.micronaut.mcp.conf.client.McpClientConnectionConfiguration;
import io.micronaut.mcp.conf.client.McpClientHttpConfiguration;
import io.micronaut.mcp.conf.client.McpClientStdioConfiguration;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Checks the connections that MCP clients are created for.
 */
@Internal
public final class McpClientConnections {
    private McpClientConnections() {
    }

    /**
     * Fails when two connections have the same name, for example {@code micronaut.mcp.client.http.foo} and
     * {@code micronaut.mcp.client.stdio.foo}, as their clients are looked up by name.
     *
     * @param connections The connections
     * @return The connections
     * @throws ConfigurationException When two connections have the same name
     */
    public static List<McpClientConnectionConfiguration> requireUniqueNames(List<McpClientConnectionConfiguration> connections) {
        Map<String, Long> counts = connections.stream()
            .collect(Collectors.groupingBy(Named::getName, Collectors.counting()));
        List<String> duplicates = counts.entrySet().stream()
            .filter(entry -> entry.getValue() > 1)
            .map(Map.Entry::getKey)
            .sorted()
            .toList();
        if (!duplicates.isEmpty()) {
            throw new ConfigurationException("The MCP connection names " + duplicates + " are declared more than once. "
                + "Each connection of " + McpClientHttpConfiguration.PREFIX + " and " + McpClientStdioConfiguration.PREFIX
                + " needs a unique name, and the deprecated langchain4j.mcp.client.transport.stdio.commands declares one named stdio.");
        }
        return connections;
    }
}
