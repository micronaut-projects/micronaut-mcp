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

import java.util.List;

/**
 * The tools of MCP servers, to offer them to a model or call them, without depending on an AI framework. The primary bean
 * of this type provides the tools of every connection, and a bean qualified by the name of a connection provides only
 * the tools of that connection. Declare {@link McpClientToolFilter} and {@link McpClientToolNameMapper} beans to select
 * and name them.
 *
 * @since 2.2.0
 */
public interface McpClientTools {

    /**
     * Lists the tools, initializing the clients that are not initialized yet.
     *
     * @return The tools
     */
    List<McpClientTool> tools();
}
