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
import io.micronaut.core.annotation.Internal;
import io.micronaut.inject.qualifiers.Qualifiers;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The tools of the server of one connection. The tools are listed once, then cached until the server notifies that they
 * changed.
 */
@Internal
final class ConnectionMcpClientTools implements McpClientTools {
    private final String client;
    private final BeanContext beanContext;
    private final McpConnectionClients connectionClients;
    private final @Nullable McpClientToolFilter filter;
    private final @Nullable McpClientToolNameMapper nameMapper;

    ConnectionMcpClientTools(String client,
                             BeanContext beanContext,
                             McpConnectionClients connectionClients,
                             @Nullable McpClientToolFilter filter,
                             @Nullable McpClientToolNameMapper nameMapper) {
        this.client = client;
        this.beanContext = beanContext;
        this.connectionClients = connectionClients;
        this.filter = filter;
        this.nameMapper = nameMapper;
    }

    @Override
    public List<McpClientTool> tools() {
        // The client is initialized when it is created, which is attempted again on the next call if it fails
        McpSyncClient mcpClient = beanContext.getBean(McpSyncClient.class, Qualifiers.byName(client));
        List<McpSchema.Tool> serverTools = connectionClients.tools(client);
        if (serverTools == null) {
            // Lists every page of tools
            serverTools = connectionClients.toolsListed(client, mcpClient.listTools().tools());
        }
        List<McpClientTool> tools = new ArrayList<>(serverTools.size());
        for (McpSchema.Tool tool : serverTools) {
            if (filter == null || filter.test(client, tool)) {
                String name = nameMapper != null ? nameMapper.apply(client, tool) : tool.name();
                tools.add(new McpClientTool(name, client, tool, mcpClient));
            }
        }
        return tools;
    }
}
