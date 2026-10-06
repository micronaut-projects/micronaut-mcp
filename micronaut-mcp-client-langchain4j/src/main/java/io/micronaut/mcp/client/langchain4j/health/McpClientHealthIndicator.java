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
import io.micronaut.context.BeanContext;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.annotation.Value;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.util.StringUtils;
import io.micronaut.health.HealthStatus;
import io.micronaut.inject.qualifiers.Qualifiers;
import io.micronaut.management.endpoint.health.HealthEndpoint;
import io.micronaut.management.health.indicator.AbstractHealthIndicator;
import io.micronaut.mcp.conf.client.McpClientConnectionConfiguration;
import io.micronaut.scheduling.TaskExecutors;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Reports whether each MCP client can reach its server, by pinging it. The status is down when any server cannot be
 * reached, including when its client cannot be created. The servers are pinged concurrently, and those that do not
 * answer within the timeout are reported down.
 *
 * @since 2.2.0
 */
@Singleton
@Internal
@Requires(classes = HealthEndpoint.class)
@Requires(beans = HealthEndpoint.class)
@Requires(beans = McpClientConnectionConfiguration.class)
@Requires(property = McpClientHealthIndicator.PROPERTY_ENABLED, notEquals = StringUtils.FALSE)
final class McpClientHealthIndicator extends AbstractHealthIndicator<Map<String, Object>> {
    /**
     * The property that disables the indicator.
     */
    static final String PROPERTY_ENABLED = HealthEndpoint.PREFIX + ".mcp.enabled";
    /**
     * The property that sets how long to wait for all the servers to answer.
     */
    static final String PROPERTY_TIMEOUT = HealthEndpoint.PREFIX + ".mcp.timeout";
    private static final String NAME = "mcp";
    private static final String STATUS = "status";
    private static final String ERROR = "error";

    private final List<McpClientConnectionConfiguration> connections;
    private final BeanContext beanContext;
    private final ExecutorService blockingExecutor;
    private final Duration timeout;

    McpClientHealthIndicator(List<McpClientConnectionConfiguration> connections,
                             BeanContext beanContext,
                             @Named(TaskExecutors.BLOCKING) ExecutorService blockingExecutor,
                             @Value("${" + PROPERTY_TIMEOUT + ":10s}") Duration timeout) {
        this.connections = connections;
        this.beanContext = beanContext;
        this.blockingExecutor = blockingExecutor;
        this.timeout = timeout;
    }

    @Override
    protected Map<String, Object> getHealthInformation() {
        Map<String, CompletableFuture<Object>> checks = new LinkedHashMap<>(connections.size());
        for (McpClientConnectionConfiguration connection : connections) {
            String name = connection.getName();
            checks.put(name, CompletableFuture.supplyAsync(() -> check(name), blockingExecutor)
                .orTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
                .exceptionally(this::down));
        }
        CompletableFuture.allOf(checks.values().toArray(CompletableFuture[]::new)).join();
        Map<String, Object> details = new LinkedHashMap<>(checks.size());
        HealthStatus status = HealthStatus.UP;
        for (Map.Entry<String, CompletableFuture<Object>> check : checks.entrySet()) {
            Object detail = check.getValue().join();
            if (!HealthStatus.UP.getName().equals(detail)) {
                status = HealthStatus.DOWN;
            }
            details.put(check.getKey(), detail);
        }
        healthStatus = status;
        return details;
    }

    private Object check(String name) {
        // The client is created on first use, which fails when the server cannot be reached
        McpClient client = beanContext.getBean(McpClient.class, Qualifiers.byName(name));
        client.checkHealth();
        return HealthStatus.UP.getName();
    }

    private Object down(Throwable error) {
        Throwable cause = error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
        String message = cause instanceof TimeoutException
            ? "No answer within " + timeout
            : String.valueOf(cause.getMessage());
        return Map.of(STATUS, HealthStatus.DOWN.getName(), ERROR, message);
    }

    @Override
    protected String getName() {
        return NAME;
    }
}
