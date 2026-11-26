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
package io.micronaut.mcp.conf.client;

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.Internal;
import io.micronaut.mcp.conf.Transport;

// Binds the deprecated transport property, until it is removed
@SuppressWarnings({"removal", "java:S5738"})
@Requires(property = McpClientConfiguration.PROPERTY_TRANSPORT)
@ConfigurationProperties(McpClientConfiguration.PREFIX)
@Internal
final class McpClientConfigurationProperties implements McpClientConfiguration {
    private Transport transport = DEFAULT_TRANSPORT;
    private boolean enabled = DEFAULT_ENABLED;

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    @Deprecated(since = "2.2.0", forRemoval = true)
    public Transport getTransport() {
        return transport;
    }

    /**
     * Set the MCP Transport. It defaults to HTTP.
     * @param transport The MCP transport.
     * @deprecated The property has no effect: the transport of each connection is set by where it is declared,
     * {@value McpClientHttpConfiguration#PREFIX} or {@value McpClientStdioConfiguration#PREFIX}.
     */
    @Deprecated(since = "2.2.0", forRemoval = true)
    public void setTransport(Transport transport) {
        this.transport = transport;
    }
}
