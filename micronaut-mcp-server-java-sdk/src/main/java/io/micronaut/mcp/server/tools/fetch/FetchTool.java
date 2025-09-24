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
package io.micronaut.mcp.server.tools.fetch;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.modelcontextprotocol.common.McpTransportContext;

import java.util.Optional;

/**
 * Tool which retrieves the full contents of a search result document or item.
 */
@FunctionalInterface
public interface FetchTool {
    String DEFAULT_NAME = "fetch";
    String DEFAULT_DESCRIPTION = "This tool retrieves the full contents of a search result document or item.";

    @NonNull
    default String getName() {
        return DEFAULT_NAME;
    }

    @NonNull
    default String getDescription() {
        return DEFAULT_DESCRIPTION;
    }

    /**
     * The fetch tool is used to retrieve the full contents of a search result document or item.
     * @param request A unique identifier of the search document
     * @param mcpTransportContext MCP Transport Context
     * @return Search Document
     */
    @NonNull
    Optional<FetchResponse> fetch(@NonNull FetchRequest request,
                                  @Nullable McpTransportContext mcpTransportContext);
}
