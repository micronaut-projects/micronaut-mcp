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
import io.micronaut.core.convert.format.MapFormat;
import io.micronaut.core.naming.conventions.StringConvention;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * {@link EachProperty} implementation of {@link McpClientStdioConfiguration}.
 */
@EachProperty(McpClientStdioConfiguration.PREFIX)
@Internal
final class McpClientStdioConfigurationProperties extends AbstractMcpClientConnectionConfigurationProperties implements McpClientStdioConfiguration {
    private List<String> command = List.of();
    private Map<String, String> environment = Map.of();
    private boolean logEvents;

    McpClientStdioConfigurationProperties(@Parameter String name) {
        super(name);
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
    public void setEnvironment(@MapFormat(transformation = MapFormat.MapTransformation.FLAT, keyFormat = StringConvention.RAW) @Nullable Map<String, String> environment) {
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
}
