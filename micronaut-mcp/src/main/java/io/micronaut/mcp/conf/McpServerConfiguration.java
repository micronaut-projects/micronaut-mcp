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
package io.micronaut.mcp.conf;

import io.micronaut.core.annotation.NonNull;

/**
 * MCP Server Configuration.
 * @since 1.0.0
 */
public interface McpServerConfiguration {
    String PREFIX = "micronaut.mcp.server";
    String PROPERTY_TRANSPORT = PREFIX + ".transport";
    String PROPERTY_REACTIVE = PREFIX + ".reactive";
    String DEFAULT_ENDPOINT = "/mcp";
    String PROPERTY_ENDPOINT = PREFIX + ".endpoint";
    String TRANSPORT_STDIO = "STDIO";
    String TRANSPORT_HTTP = "HTTP";
    boolean DEFAULT_REACTIVE = false;
    Transport DEFAULT_TRANSPORT = Transport.HTTP;

    /**
     *
     * @return MCP Server endpoint. It applies to MCP Servers using HTTP transport. It defaults to {@value #DEFAULT_ENDPOINT}.
     */
    @NonNull
    default String getEndpoint() {
        return DEFAULT_ENDPOINT;
    }

    /**
     * @return The MCP Transport.
     */
    @NonNull
    default Transport getTransport() {
        return DEFAULT_TRANSPORT;
    }

    /**
     *
     * @return Whether you want to define MCP Primitive handlers using reactive code.
     */
    default boolean isReactive() {
        return DEFAULT_REACTIVE;
    }
}
