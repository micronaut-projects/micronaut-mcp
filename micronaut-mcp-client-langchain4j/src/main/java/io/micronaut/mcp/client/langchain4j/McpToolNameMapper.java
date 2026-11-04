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

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.mcp.client.McpClient;

import java.util.function.BiFunction;

/**
 * Names the tools of the MCP clients that the {@link dev.langchain4j.mcp.McpToolProvider} beans provide, for example to
 * prefix them with the client key so that tools of different servers cannot clash. Declare a bean of this type to rename them.
 *
 * @since 2.2.0
 */
@FunctionalInterface
public interface McpToolNameMapper extends BiFunction<McpClient, ToolSpecification, String> {
}
