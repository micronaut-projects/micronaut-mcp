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

import io.micronaut.context.annotation.EachProperty;
import io.micronaut.context.annotation.Parameter;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;

import java.net.URI;
import java.time.Duration;

/**
 * {@link EachProperty} implementation of {@link McpClientHttpConfiguration}.
 */
@EachProperty(McpClientHttpConfiguration.PREFIX)
@Internal
final class McpClientHttpConfigurationProperties implements McpClientHttpConfiguration {
    private final String name;
    private URI url;
    private Duration timeout;
    private boolean logRequests;
    private boolean logResponses;

    /**
     * Constructor.
     * @param name Name Qualifier
     */
    McpClientHttpConfigurationProperties(@Parameter String name) {
        this.name = name;
    }

    /**
     *
     * @return The name qualifier
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     *
     * @return The MCP Server URL
     */
    @Override
    @NonNull
    public URI getUrl() {
        return url;
    }

    /**
     *
     * @param url The MCP Server URL
     */
    public void setUrl(@NonNull URI url) {
        this.url = url;
    }

    @Override
    @Nullable
    public Duration getTimeout() {
        return timeout;
    }

    /**
     *
     * @param timeout The duration to wait for server responses before timing out requests.
     */
    public void setTimeout(@Nullable Duration timeout) {
        this.timeout = timeout;
    }

    @Override
    public boolean isLogRequests() {
        return logRequests;
    }

    /**
     * Whether to log requests. Default value `false`.
     * @param logRequests Whether to log requests
     */
    public void setLogRequests(boolean logRequests) {
        this.logRequests = logRequests;
    }

    @Override
    public boolean isLogResponses() {
        return logResponses;
    }

    /**
     * Whether to log responses. Default value `false`.
     * @param logResponses Whether to log responses
     */
    public void setLogResponses(boolean logResponses) {
        this.logResponses = logResponses;
    }
}
