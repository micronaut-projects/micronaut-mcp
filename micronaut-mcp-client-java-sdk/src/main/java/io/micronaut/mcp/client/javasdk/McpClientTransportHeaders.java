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
package io.micronaut.mcp.client.javasdk;

import io.modelcontextprotocol.common.McpTransportContext;
import org.jspecify.annotations.NonNull;
import reactor.util.context.Context;

import java.util.Map;

/**
 * Supplies the headers of the requests that the MCP clients of HTTP connections send, through the
 * {@link McpTransportContext}. The synchronous client computes them on the thread that calls it. Requests of the
 * asynchronous client compute them from the Micronaut context propagated in the Reactor context of their subscriber,
 * such as the HTTP request a reactive controller is handling, or take them from the Reactor context, for example:
 *
 * <pre>{@code
 * client.callTool(request)
 *     .contextWrite(context -> McpClientTransportHeaders.withHeaders(context, Map.of("Authorization", "Bearer " + token)));
 * }</pre>
 *
 * <p>The headers supplied this way are sent in addition to the static headers of the connection, instead of those of the
 * {@link io.micronaut.mcp.conf.client.McpClientHeadersProvider headers providers}.</p>
 *
 * @since 2.2.0
 */
public final class McpClientTransportHeaders {
    /**
     * The key of the {@link McpTransportContext} entry, a {@code Map<String, String>}, that holds the headers to send.
     */
    public static final String HEADERS = "io.micronaut.mcp.client.javasdk.headers";

    private McpClientTransportHeaders() {
    }

    /**
     * @param headers The headers to send
     * @return A transport context that holds the headers
     */
    public static @NonNull McpTransportContext transportContext(@NonNull Map<String, String> headers) {
        return McpTransportContext.create(Map.of(HEADERS, Map.copyOf(headers)));
    }

    /**
     * Adds the headers to send to a Reactor context, as the transport context that the asynchronous client reads from
     * the context of its subscriber.
     *
     * @param context The Reactor context
     * @param headers The headers to send
     * @return The Reactor context, with the headers
     */
    public static @NonNull Context withHeaders(@NonNull Context context, @NonNull Map<String, String> headers) {
        return context.put(McpTransportContext.KEY, transportContext(headers));
    }
}
