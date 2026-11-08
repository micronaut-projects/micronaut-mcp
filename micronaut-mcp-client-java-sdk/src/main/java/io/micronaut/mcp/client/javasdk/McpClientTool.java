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

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;

import java.util.Map;

/**
 * A tool of an MCP server.
 *
 * @param name The name to offer the tool under, which an {@link McpClientToolNameMapper} may have changed
 * @param client The name of the connection to the server
 * @param tool The tool, as the server lists it
 * @param mcpClient The client of the server
 * @since 2.2.0
 */
public record McpClientTool(String name, String client, McpSchema.Tool tool, McpSyncClient mcpClient) {

    /**
     * Calls the tool.
     *
     * @param arguments The arguments
     * @return The result
     */
    public McpSchema.CallToolResult call(Map<String, Object> arguments) {
        return mcpClient.callTool(new McpSchema.CallToolRequest(tool.name(), arguments));
    }
}
