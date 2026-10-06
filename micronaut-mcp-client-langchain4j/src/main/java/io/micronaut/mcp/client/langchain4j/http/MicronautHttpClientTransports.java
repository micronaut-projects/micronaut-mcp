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
package io.micronaut.mcp.client.langchain4j.http;

import dev.langchain4j.mcp.client.transport.McpTransport;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.Internal;
import io.micronaut.http.client.StreamingHttpClient;
import io.micronaut.http.client.StreamingHttpClientRegistry;
import io.micronaut.mcp.client.http.MicronautMcpHttpExchange;
import io.micronaut.mcp.conf.client.McpClientHeadersProvider;
import io.micronaut.mcp.conf.client.McpClientHttpConfiguration;
import jakarta.inject.Singleton;

import java.util.List;

/**
 * Creates the transports of the HTTP connections that use the Micronaut HTTP client.
 */
@Singleton
@Internal
@Requires(classes = StreamingHttpClient.class)
public final class MicronautHttpClientTransports {
    private final StreamingHttpClientRegistry<?> registry;
    private final List<McpClientHeadersProvider> headersProviders;

    MicronautHttpClientTransports(StreamingHttpClientRegistry<?> registry, List<McpClientHeadersProvider> headersProviders) {
        this.registry = registry;
        this.headersProviders = headersProviders;
    }

    /**
     * @param configuration The connection
     * @return The transport of the connection
     */
    public McpTransport create(McpClientHttpConfiguration configuration) {
        return new MicronautStreamableHttpMcpTransport(new MicronautMcpHttpExchange(registry, configuration, headersProviders));
    }
}
