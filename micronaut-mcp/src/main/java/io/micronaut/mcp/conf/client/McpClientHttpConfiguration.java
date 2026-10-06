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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

/**
 * MCP Client HTTP Configuration.
 */
public interface McpClientHttpConfiguration extends McpClientConnectionConfiguration {
    String PREFIX = "micronaut.mcp.client.http";

    /**
     *
     * @return The URL of the MCP Server
     */
    @NonNull
    URI getUrl();

    /**
     *
     * @return Sets the duration to wait for server responses before timing out requests.
     */
    @Nullable
    Duration getTimeout();

    /**
     *
     * @return Whether to log requests
     */
    boolean isLogRequests();

    /**
     *
     * @return Whether to log responses
     */
    boolean isLogResponses();

    /**
     * @return The headers sent with every request, such as {@code Authorization}
     * @since 2.2.0
     */
    @NonNull
    default Map<String, String> getHeaders() {
        return Map.of();
    }

    /**
     * @return Whether to send the {@code Authorization} header of the HTTP request the server is handling, for example the
     * bearer token of the user, to the MCP server
     * @since 2.2.0
     */
    default boolean isPropagateAuthorization() {
        return false;
    }

    /**
     * @return Whether to {@link #isPropagateAuthorization() propagate} the {@code Authorization} header whatever its
     * scheme, for example Basic credentials, instead of only bearer tokens
     * @since 2.2.0
     */
    default boolean isPropagateAnyAuthorizationScheme() {
        return false;
    }

    @NonNull
    static McpClientHttpConfiguration of(@NonNull String name,
                                           @NonNull URI url) {
        McpClientHttpConfigurationProperties config = new McpClientHttpConfigurationProperties(name);
        config.setUrl(url);
        return config;
    }
}
