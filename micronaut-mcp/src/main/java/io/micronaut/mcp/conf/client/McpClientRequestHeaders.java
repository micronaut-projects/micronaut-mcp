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

import io.micronaut.core.annotation.Internal;
import org.jspecify.annotations.NonNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Computes the headers of a request an MCP client sends over HTTP.
 */
@Internal
public final class McpClientRequestHeaders {
    private McpClientRequestHeaders() {
    }

    /**
     * @param connection The connection
     * @param providers The headers providers
     * @return Whether the headers of the requests of the connection must be computed for each request
     */
    public static boolean isDynamic(@NonNull McpClientHttpConfiguration connection, @NonNull List<McpClientHeadersProvider> providers) {
        return connection.isPropagateAuthorization() || providers.stream().anyMatch(p -> !(p instanceof AuthorizationPropagationHeadersProvider) && p.supports(connection));
    }

    /**
     * @param connection The connection
     * @param providers The headers providers
     * @return The static headers of the connection, overridden by those of the providers
     */
    public static @NonNull Map<String, String> headers(@NonNull McpClientHttpConfiguration connection, @NonNull List<McpClientHeadersProvider> providers) {
        Map<String, String> headers = merge(new LinkedHashMap<>(), connection.getHeaders());
        for (McpClientHeadersProvider provider : providers) {
            if (provider.supports(connection)) {
                merge(headers, provider.headers(connection));
            }
        }
        return headers;
    }

    /**
     * Puts headers into others, replacing those with the same name whatever its case, as HTTP field names are case
     * insensitive: otherwise both {@code authorization} and {@code Authorization} would be sent.
     *
     * @param headers The headers, which are changed
     * @param overrides The headers that replace those with the same name
     * @return The headers
     */
    public static @NonNull Map<String, String> merge(@NonNull Map<String, String> headers, @NonNull Map<String, String> overrides) {
        for (Map.Entry<String, String> override : overrides.entrySet()) {
            headers.keySet().removeIf(name -> name.equalsIgnoreCase(override.getKey()));
            headers.put(override.getKey(), override.getValue());
        }
        return headers;
    }
}
