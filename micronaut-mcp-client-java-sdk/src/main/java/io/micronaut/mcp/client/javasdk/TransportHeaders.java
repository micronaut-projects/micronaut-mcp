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

import io.micronaut.core.annotation.Internal;
import io.micronaut.core.async.propagation.ReactorPropagation;
import io.micronaut.core.propagation.PropagatedContext;
import io.micronaut.mcp.conf.client.McpClientHeadersProvider;
import io.micronaut.mcp.conf.client.McpClientHttpConfiguration;
import io.micronaut.mcp.conf.client.McpClientRequestHeaders;
import io.modelcontextprotocol.common.McpTransportContext;
import reactor.util.context.ContextView;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Computes the headers of a request of a Java SDK client over HTTP.
 */
@Internal
final class TransportHeaders {
    private TransportHeaders() {
    }

    /**
     * The headers of a request: those of the transport context, which the synchronous client computes on the thread
     * that calls it, or that the application supplies, else those computed in the Micronaut context propagated in the
     * Reactor context of the subscriber, else those computed on the current thread.
     *
     * @param configuration The connection
     * @param headersProviders The headers providers
     * @param context The transport context
     * @param reactorContext The Reactor context of the subscriber
     * @return The headers
     */
    static Map<String, String> headers(McpClientHttpConfiguration configuration,
                                       List<McpClientHeadersProvider> headersProviders,
                                       McpTransportContext context,
                                       ContextView reactorContext) {
        if (context.get(McpClientTransportHeaders.HEADERS) instanceof Map<?, ?> supplied) {
            Map<String, String> headers = new LinkedHashMap<>(configuration.getHeaders());
            supplied.forEach((name, value) -> headers.put(name.toString(), value.toString()));
            return headers;
        }
        if (!McpClientRequestHeaders.isDynamic(configuration, headersProviders)) {
            return configuration.getHeaders();
        }
        // Without a propagated context, for example for the answers to the requests of the server, the headers providers
        // run without the HTTP request the application is handling
        Optional<PropagatedContext> propagatedContext = ReactorPropagation.findPropagatedContext(reactorContext);
        if (propagatedContext.isPresent()) {
            try (PropagatedContext.Scope ignored = propagatedContext.get().propagate()) {
                return McpClientRequestHeaders.headers(configuration, headersProviders);
            }
        }
        return McpClientRequestHeaders.headers(configuration, headersProviders);
    }
}
