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
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.util.StringUtils;
import io.micronaut.health.HealthStatus;
import io.micronaut.inject.qualifiers.Qualifiers;
import io.micronaut.management.endpoint.health.HealthEndpoint;
import io.micronaut.management.health.indicator.AbstractHealthIndicator;
import io.micronaut.mcp.client.langchain4j.McpClientConnections;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Reports whether each MCP client can reach its server, by pinging it. The status is down when any server cannot be
 * reached, including when its client cannot be created. The servers are pinged concurrently, and those that do not
 * answer within the timeout are reported down. A ping that is still running when the indicator is asked again, for
 * example behind the creation of a client whose server hangs, is awaited instead of starting another one.
 *
 * @since 2.2.0
 */
@Singleton
@Internal
@Requires(classes = HealthEndpoint.class)
@Requires(beans = HealthEndpoint.class)
@Requires(beans = McpClientConnectionConfiguration.class)
@Requires(property = McpClientHealthConfiguration.PROPERTY_ENABLED, notEquals = StringUtils.FALSE)
final class McpClientHealthIndicator extends AbstractHealthIndicator<Map<String, Object>> {
    private static final String NAME = "mcp";
    private static final String STATUS = "status";
    private static final String ERROR = "error";

    private final List<McpClientConnectionConfiguration> connections;
    private final BeanContext beanContext;
    private final ExecutorService blockingExecutor;
    private final Duration timeout;
    private final Map<String, CompletableFuture<Map<String, String>>> pings = new ConcurrentHashMap<>();

    McpClientHealthIndicator(List<McpClientConnectionConfiguration> connections,
                             BeanContext beanContext,
                             @Named(TaskExecutors.BLOCKING) ExecutorService blockingExecutor,
                             McpClientHealthConfiguration configuration) {
        this.connections = McpClientConnections.requireUniqueNames(connections);
        this.beanContext = beanContext;
        this.blockingExecutor = blockingExecutor;
        this.timeout = configuration.getTimeout();
    }

    @Override
    protected Map<String, Object> getHealthInformation() {
        Map<String, CompletableFuture<Map<String, String>>> checks = LinkedHashMap.newLinkedHashMap(connections.size());
        for (McpClientConnectionConfiguration connection : connections) {
            String name = connection.getName();
            // The timeout completes a copy, so that the ping itself is not completed and is awaited by the next check
            checks.put(name, ping(name).copy()
                .orTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
                .exceptionally(this::down));
        }
        CompletableFuture.allOf(checks.values().toArray(CompletableFuture[]::new)).join();
        Map<String, Object> details = LinkedHashMap.newLinkedHashMap(checks.size());
        HealthStatus status = HealthStatus.UP;
        for (Map.Entry<String, CompletableFuture<Map<String, String>>> check : checks.entrySet()) {
            Map<String, String> detail = check.getValue().join();
            if (!HealthStatus.UP.getName().equals(detail.get(STATUS))) {
                status = HealthStatus.DOWN;
            }
            details.put(check.getKey(), detail);
        }
        healthStatus = status;
        return details;
    }

    private CompletableFuture<Map<String, String>> ping(String name) {
        return pings.compute(name, (key, running) -> running != null && !running.isDone()
            ? running
            : CompletableFuture.supplyAsync(() -> check(key), blockingExecutor));
    }

    private Map<String, String> check(String name) {
        // The client is created on first use, which fails when the server cannot be reached
        McpClient client = beanContext.getBean(McpClient.class, Qualifiers.byName(name));
        client.checkHealth();
        return Map.of(STATUS, HealthStatus.UP.getName());
    }

    private Map<String, String> down(Throwable error) {
        Throwable cause = error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
        String message;
        if (cause instanceof TimeoutException) {
            message = "No answer within " + format(timeout) + ", the client may still be connecting to its server. See "
                + McpClientHealthConfiguration.PREFIX + ".timeout";
        } else if (cause.getMessage() != null) {
            message = cause.getMessage();
        } else {
            // Some errors have no message, their type still tells what failed
            message = cause.getClass().getName();
        }
        return Map.of(STATUS, HealthStatus.DOWN.getName(), ERROR, message);
    }

    private static String format(Duration duration) {
        long millis = duration.toMillis();
        return millis % 1000 == 0 ? (millis / 1000) + "s" : millis + "ms";
    }

    @Override
    protected String getName() {
        return NAME;
    }
}
