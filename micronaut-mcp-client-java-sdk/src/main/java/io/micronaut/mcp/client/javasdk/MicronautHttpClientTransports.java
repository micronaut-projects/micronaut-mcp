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

import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.Internal;
import io.micronaut.http.client.StreamingHttpClient;
import io.micronaut.mcp.client.http.MicronautMcpHttpClients;
import io.micronaut.mcp.conf.client.McpClientHeadersProvider;
import io.micronaut.mcp.conf.client.McpClientHttpConfiguration;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.spec.McpClientTransport;
import jakarta.inject.Singleton;

import java.util.List;

/**
 * Creates the transports of the HTTP connections that use the Micronaut HTTP client.
 */
@Singleton
@Internal
@Requires(classes = StreamingHttpClient.class)
final class MicronautHttpClientTransports {
    private final MicronautMcpHttpClients clients;
    private final McpJsonMapper jsonMapper;
    private final List<McpClientHeadersProvider> headersProviders;

    MicronautHttpClientTransports(MicronautMcpHttpClients clients,
                                  McpJsonMapper jsonMapper,
                                  List<McpClientHeadersProvider> headersProviders) {
        this.clients = clients;
        this.jsonMapper = jsonMapper;
        this.headersProviders = headersProviders;
    }

    McpClientTransport create(McpClientHttpConfiguration configuration) {
        return new MicronautStreamableHttpClientTransport(clients.exchange(configuration), configuration, headersProviders, jsonMapper);
    }
}
