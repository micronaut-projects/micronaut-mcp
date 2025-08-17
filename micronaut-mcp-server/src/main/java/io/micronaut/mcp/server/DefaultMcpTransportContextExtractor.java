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
package io.micronaut.mcp.server;

import io.micronaut.core.annotation.Internal;
import io.micronaut.http.HttpRequest;
import io.modelcontextprotocol.server.McpTransportContext;
import io.modelcontextprotocol.server.McpTransportContextExtractor;
import jakarta.inject.Singleton;

/**
 * Default implementation of {@link McpTransportContextExtractor}.
 */
@Internal
@Singleton
final class DefaultMcpTransportContextExtractor implements McpTransportContextExtractor<HttpRequest<?>> {
    public static final String HTTP_HEADER_MCP_PROTOCOL_VERSION = "MCP-Protocol-Version";
    public static final String DEFAULT_PROTOCOL_VERSION = "2025-03-26";
    public static final String HTTP_HEADER_MCP_SESSION_ID = "Mcp-Session-Id";
    public static final String HTTP_HEADER_DEFAULT_LAST_EVENT_ID = "Last-Event-ID";

    @Override
    public McpTransportContext extract(HttpRequest<?> request, McpTransportContext transportContext) {
        transportContext.put(HTTP_HEADER_MCP_PROTOCOL_VERSION,
            request.getHeaders().get(HTTP_HEADER_MCP_PROTOCOL_VERSION, String.class)
                .orElse(DEFAULT_PROTOCOL_VERSION));
        request.getHeaders().get(HTTP_HEADER_MCP_SESSION_ID, String.class)
            .ifPresent(v -> transportContext.put(HTTP_HEADER_MCP_SESSION_ID, v));
        request.getHeaders().get(HTTP_HEADER_DEFAULT_LAST_EVENT_ID, String.class)
            .ifPresent(v -> transportContext.put(HTTP_HEADER_DEFAULT_LAST_EVENT_ID, v));
        return transportContext;
    }
}
