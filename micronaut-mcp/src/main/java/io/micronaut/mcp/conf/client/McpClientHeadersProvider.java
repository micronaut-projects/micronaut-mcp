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

import org.jspecify.annotations.NonNull;

import java.util.Map;

/**
 * Provides headers for the requests MCP clients send over HTTP, computed for each request, for example to send an OAuth
 * access token that is refreshed, or a token exchanged for one whose audience is the MCP server. Every bean of this type
 * that {@link #supports(McpClientHttpConfiguration) supports} a connection is asked, on the thread that calls the client,
 * in addition to the static {@link McpClientHttpConfiguration#getHeaders() headers} of the connection, which its headers
 * override.
 *
 * <p>A provider applies to every connection unless it overrides {@link #supports(McpClientHttpConfiguration)}: check the
 * connection, so that a token meant for one MCP server is not sent to the others.</p>
 *
 * <p>A provider must also handle there being no HTTP request the application is handling, for example when the client
 * initializes or pings the server, or answers a request of the server such as sampling, which it does on threads other
 * than the one that called it.</p>
 *
 * @since 2.2.0
 */
@FunctionalInterface
public interface McpClientHeadersProvider {

    /**
     * @param connection The connection the request is sent over
     * @return The headers to send, empty for none
     */
    @NonNull
    Map<String, String> headers(@NonNull McpClientHttpConfiguration connection);

    /**
     * @param connection The connection
     * @return Whether this provider provides headers for the requests sent over the connection
     */
    default boolean supports(@NonNull McpClientHttpConfiguration connection) {
        return true;
    }
}
