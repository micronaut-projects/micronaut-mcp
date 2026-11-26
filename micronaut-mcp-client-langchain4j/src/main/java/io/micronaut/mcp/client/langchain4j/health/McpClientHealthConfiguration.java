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
package io.micronaut.mcp.client.langchain4j.health;

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.Internal;
import io.micronaut.management.endpoint.health.HealthEndpoint;

import java.time.Duration;

/**
 * The configuration of the {@code mcp} health indicator.
 */
@ConfigurationProperties(McpClientHealthConfiguration.PREFIX)
@Requires(classes = HealthEndpoint.class)
@Internal
final class McpClientHealthConfiguration {
    /**
     * The prefix of the properties.
     */
    static final String PREFIX = HealthEndpoint.PREFIX + ".mcp";
    /**
     * The property that disables the indicator.
     */
    static final String PROPERTY_ENABLED = PREFIX + ".enabled";
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);

    private boolean enabled = true;
    private Duration timeout = DEFAULT_TIMEOUT;

    /**
     * @return Whether the indicator is enabled
     */
    boolean isEnabled() {
        return enabled;
    }

    /**
     * Whether the `mcp` health indicator pings the MCP servers. When it is enabled, a server that cannot be reached
     * makes the health endpoint answer 503. Default value `true`.
     *
     * @param enabled Whether the indicator is enabled
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * @return How long to wait for the servers to answer
     */
    Duration getTimeout() {
        return timeout;
    }

    /**
     * How long the `mcp` health indicator waits for the MCP servers to answer its pings. Default value `10s`.
     *
     * @param timeout How long to wait for the servers to answer
     */
    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }
}
