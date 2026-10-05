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

import dev.langchain4j.mcp.client.McpClient;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.util.StringUtils;
import io.micronaut.health.HealthStatus;
import io.micronaut.management.endpoint.health.HealthEndpoint;
import io.micronaut.management.health.indicator.AbstractHealthIndicator;
import jakarta.inject.Singleton;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reports whether each MCP client can reach its server, by pinging it. The status is down when any server cannot be reached.
 *
 * @since 2.2.0
 */
@Singleton
@Internal
@Requires(classes = HealthEndpoint.class)
@Requires(beans = HealthEndpoint.class)
@Requires(beans = McpClient.class)
@Requires(property = McpClientHealthIndicator.PROPERTY_ENABLED, notEquals = StringUtils.FALSE)
final class McpClientHealthIndicator extends AbstractHealthIndicator<Map<String, Object>> {
    /**
     * The property that disables the indicator.
     */
    static final String PROPERTY_ENABLED = HealthEndpoint.PREFIX + ".mcp.enabled";
    private static final String NAME = "mcp";

    private final List<McpClient> clients;

    McpClientHealthIndicator(List<McpClient> clients) {
        this.clients = clients;
    }

    @Override
    protected Map<String, Object> getHealthInformation() {
        Map<String, Object> details = new LinkedHashMap<>(clients.size());
        HealthStatus status = HealthStatus.UP;
        for (McpClient client : clients) {
            try {
                client.checkHealth();
                details.put(client.key(), HealthStatus.UP.getName());
            } catch (RuntimeException e) {
                status = HealthStatus.DOWN;
                details.put(client.key(), Map.of("status", HealthStatus.DOWN.getName(), "error", String.valueOf(e.getMessage())));
            }
        }
        healthStatus = status;
        return details;
    }

    @Override
    protected String getName() {
        return NAME;
    }
}
