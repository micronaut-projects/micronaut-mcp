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
package io.micronaut.mcp.conf.client;

import io.micronaut.context.annotation.EachProperty;
import io.micronaut.context.annotation.Parameter;
import io.micronaut.core.annotation.Internal;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * {@link EachProperty} implementation of {@link McpClientStdioConfiguration}.
 */
@EachProperty(McpClientStdioConfiguration.PREFIX)
@Internal
final class McpClientStdioConfigurationProperties implements McpClientStdioConfiguration {
    private final String name;
    private List<String> command = List.of();
    private Map<String, String> environment = Map.of();
    private boolean logEvents;
    private @Nullable Duration initializationTimeout;
    private @Nullable Duration requestTimeout;
    private boolean autoHealthCheck = true;

    McpClientStdioConfigurationProperties(@Parameter String name) {
        this.name = name;
    }

    @Override
    public @NonNull String getName() {
        return name;
    }

    @Override
    public @NonNull List<String> getCommand() {
        return command;
    }

    /**
     * @param command The command that starts the server, and its arguments
     */
    public void setCommand(@NonNull List<String> command) {
        this.command = command;
    }

    @Override
    public @NonNull Map<String, String> getEnvironment() {
        return environment;
    }

    /**
     * @param environment The environment variables of the server process, in addition to those of this process
     */
    public void setEnvironment(@Nullable Map<String, String> environment) {
        this.environment = environment != null ? environment : Map.of();
    }

    @Override
    public boolean isLogEvents() {
        return logEvents;
    }

    /**
     * Whether to log the messages exchanged with the server. Default value {@code false}.
     *
     * @param logEvents Whether to log the messages
     */
    public void setLogEvents(boolean logEvents) {
        this.logEvents = logEvents;
    }

    @Override
    public @Nullable Duration getInitializationTimeout() {
        return initializationTimeout;
    }

    /**
     * @param initializationTimeout How long to wait for the server to answer the initialization
     */
    public void setInitializationTimeout(@Nullable Duration initializationTimeout) {
        this.initializationTimeout = initializationTimeout;
    }

    @Override
    public @Nullable Duration getRequestTimeout() {
        return requestTimeout;
    }

    /**
     * @param requestTimeout How long to wait for the server to answer a request, such as a tool call
     */
    public void setRequestTimeout(@Nullable Duration requestTimeout) {
        this.requestTimeout = requestTimeout;
    }

    @Override
    public boolean isAutoHealthCheck() {
        return autoHealthCheck;
    }

    /**
     * Whether the LangChain4j client checks periodically that the server is reachable, and reconnects when it is not.
     * Default value {@code true}.
     *
     * @param autoHealthCheck Whether to check the server periodically
     */
    public void setAutoHealthCheck(boolean autoHealthCheck) {
        this.autoHealthCheck = autoHealthCheck;
    }
}
