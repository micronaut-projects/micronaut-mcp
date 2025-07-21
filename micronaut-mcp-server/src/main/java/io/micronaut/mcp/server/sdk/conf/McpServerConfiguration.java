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
package io.micronaut.mcp.server.sdk.conf;

/**
 * MCP Server Configuration.
 */
public interface McpServerConfiguration {
    String PREFIX = "micronaut.mcp.server";
    String PROPERTY_ASYNC = PREFIX + "async";
    boolean DEFAULT_ASYNC = false;

    default boolean isAsync() {
        return DEFAULT_ASYNC;
    }
}
