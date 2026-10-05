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

import java.util.function.BiFunction;

/**
 * Names the tools {@link McpClientTools} provides, given the name of the connection to the server and the tool, for
 * example to prefix them with the connection name so that the tools of different servers cannot clash.
 *
 * @since 2.2.0
 */
@FunctionalInterface
public interface McpClientToolNameMapper extends BiFunction<String, McpSchema.Tool, String> {
}
