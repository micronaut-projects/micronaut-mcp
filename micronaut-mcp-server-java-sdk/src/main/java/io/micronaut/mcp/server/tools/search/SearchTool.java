/*
 * Copyright 2017-2025 original authors
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
package io.micronaut.mcp.server.tools.search;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.modelcontextprotocol.common.McpTransportContext;

/**
 *  The search tool is responsible for returning a list of relevant search results from your MCP server's data source, given a user's query.
 *  <a href="https://platform.openai.com/docs/mcp#search-tool">search tool</a>
 */
@FunctionalInterface
public interface SearchTool {
    String DEFAULT_NAME = "search";
    String DEFAULT_TITLE = "Search";
    String DEFAULT_DESCRIPTION = "Returns a list of relevant search results, given a user's query.";

    /**
     *
     * @return MCP Tool Name
     */
    @NonNull
    default String getName() {
        return DEFAULT_NAME;
    }

    /**
     *
     * @return MCP Tool Title
     */
    @NonNull
    default String getTitle() {
        return DEFAULT_TITLE;
    }

    /**
     *
     * @return MCP Tool Description
     */
    @NonNull
    default String getDescription() {
        return DEFAULT_DESCRIPTION;
    }

    /**
     *
     * @param request A single query string.
     * @param mcpTransportContext MCP Transport Context
     * @return An object with a single key, results, whose value is an array of result objects.
     */
    @NonNull
    SearchResponse search(@NonNull SearchRequest request,
                          @Nullable McpTransportContext mcpTransportContext);
}
