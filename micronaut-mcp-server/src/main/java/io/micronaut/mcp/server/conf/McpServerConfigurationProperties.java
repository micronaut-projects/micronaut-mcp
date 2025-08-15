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
package io.micronaut.mcp.server.conf;

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.Internal;

@Requires(property = McpServerConfiguration.PROPERTY_TYPE)
@ConfigurationProperties(McpServerConfiguration.PREFIX)
@Internal
final class McpServerConfigurationProperties implements McpServerConfiguration {
    private ServerType type;
    private String endpoint = DEFAULT_ENDPOINT;

    @Override
    public String getEndpoint() {
        return endpoint;
    }

    /**
     *
     * @param endpoint The MCP Server endpoint. It applies to MCP Servers using HTTP transport. It defaults to `/mcp`.
     */
    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    @Override
    public ServerType getType() {
        return type;
    }

    /**
     *
     * @param type The type of MCP Server you want to instantiate.
     */
    public void setType(ServerType type) {
        this.type = type;
    }
}
