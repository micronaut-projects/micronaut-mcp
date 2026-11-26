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
import io.micronaut.mcp.client.langchain4j.stdio.LegacyMcpClientStdioConfiguration;
import io.micronaut.mcp.conf.client.McpClientConnectionConfiguration;
import io.micronaut.mcp.conf.client.McpClientHttpConfiguration;
import io.micronaut.mcp.conf.client.McpClientStdioConfiguration;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
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
        Map<String, List<String>> declarations = connections.stream()
            .collect(Collectors.groupingBy(Named::getName, TreeMap::new,
                Collectors.mapping(McpClientConnections::declaration, Collectors.toList())));
        List<String> clashes = declarations.entrySet().stream()
            .filter(entry -> entry.getValue().size() > 1)
            .map(entry -> String.join(" and ", entry.getValue().stream().sorted().toList()) + " have the same name '" + entry.getKey() + "'")
            .toList();
        if (!clashes.isEmpty()) {
            throw new ConfigurationException("Each MCP connection needs a unique name, but " + String.join(", ", clashes) + ". Rename one of them.");
        }
        return connections;
    }

    private static String declaration(McpClientConnectionConfiguration connection) {
        if (connection instanceof McpClientHttpConfiguration) {
            return McpClientHttpConfiguration.PREFIX + "." + connection.getName();
        } else if (connection instanceof McpClientStdioConfiguration) {
            return connection instanceof LegacyMcpClientStdioConfiguration
                ? LegacyMcpClientStdioConfiguration.PROPERTY
                : McpClientStdioConfiguration.PREFIX + "." + connection.getName();
        }
        return "the bean " + connection.getClass().getName();
    }
}
