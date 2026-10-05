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

import io.micronaut.core.annotation.Internal;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The tools of the server of one connection.
 */
@Internal
final class ConnectionMcpClientTools implements McpClientTools {
    private final String client;
    private final McpSyncClient mcpClient;
    private final @Nullable McpClientToolFilter filter;
    private final @Nullable McpClientToolNameMapper nameMapper;

    ConnectionMcpClientTools(String client,
                             McpSyncClient mcpClient,
                             @Nullable McpClientToolFilter filter,
                             @Nullable McpClientToolNameMapper nameMapper) {
        this.client = client;
        this.mcpClient = mcpClient;
        this.filter = filter;
        this.nameMapper = nameMapper;
    }

    @Override
    public List<McpClientTool> tools() {
        if (!mcpClient.isInitialized()) {
            mcpClient.initialize();
        }
        List<McpClientTool> tools = new ArrayList<>();
        String cursor = null;
        do {
            McpSchema.ListToolsResult page = cursor == null ? mcpClient.listTools() : mcpClient.listTools(cursor);
            for (McpSchema.Tool tool : page.tools()) {
                if (filter == null || filter.test(client, tool)) {
                    String name = nameMapper != null ? nameMapper.apply(client, tool) : tool.name();
                    tools.add(new McpClientTool(name, client, tool, mcpClient));
                }
            }
            cursor = page.nextCursor();
        } while (cursor != null);
        return tools;
    }
}
