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

import io.modelcontextprotocol.spec.McpSchema;

import java.util.List;

/**
 * Notified when an MCP server changes its tools, prompts or resources.
 *
 * @since 2.2.0
 */
public interface McpListChangedListener {

    /**
     * @param client The name of the connection to the server
     * @param tools The tools of the server
     */
    default void toolsChanged(String client, List<McpSchema.Tool> tools) {
    }

    /**
     * @param client The name of the connection to the server
     * @param prompts The prompts of the server
     */
    default void promptsChanged(String client, List<McpSchema.Prompt> prompts) {
    }

    /**
     * @param client The name of the connection to the server
     * @param resources The resources of the server
     */
    default void resourcesChanged(String client, List<McpSchema.Resource> resources) {
    }
}
