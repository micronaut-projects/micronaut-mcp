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
 * @since 1.0.0
 */
public interface McpServerConfiguration {
    String PREFIX = "micronaut.mcp.server";
    String PROPERTY_TYPE = PREFIX + ".type";
    String TYPE_SYNC = "SYNC";
    String TYPE_ASYNC = "ASYNC";
    String TYPE_STATELESS_SYNC = "STATELESS_SYNC";
    String TYPE_STATELESS_ASYNC = "STATELESS_ASYNC";
    ServerType DEFAULT_TYPE = ServerType.STATELESS_SYNC;
    String DEFAULT_ENDPOINT = "/mcp";
    String PROPERTY_ENDPOINT = PREFIX + ".endpoint";

    /**
     *
     * @return MCP Server endpoint. It applies to MCP Servers using HTTP transport. It defaults to {@value #DEFAULT_ENDPOINT}.
     */
    default String getEndpoint() {
        return DEFAULT_ENDPOINT;
    }

    /**
     * It defaults to {@value #TYPE_STATELESS_SYNC}.
     * It should be either {@value TYPE_SYNC} or {@value TYPE_ASYNC} for stdio transport.
     * It should be either {@value TYPE_STATELESS_ASYNC} or {@value TYPE_STATELESS_SYNC} for HTTP transport.
     * @return The MCP Server Type.
     */
    default ServerType getType() {
        return DEFAULT_TYPE;
    }
}
