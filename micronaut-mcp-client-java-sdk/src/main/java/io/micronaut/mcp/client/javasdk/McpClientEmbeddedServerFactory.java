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
package io.micronaut.mcp.client.javasdk;

import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.Internal;
import io.micronaut.mcp.conf.McpServerConfiguration;
import io.micronaut.runtime.server.EmbeddedServer;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

@Requires(classes = { EmbeddedServer.class, McpServerConfiguration.class })
@Requires(beans = { EmbeddedServer.class, McpServerConfiguration.class })
@Internal
@Factory
final class McpClientEmbeddedServerFactory {
    @Requires(beans = EmbeddedServer.class)
    @Named("embeddedServer")
    @Singleton
    HttpClientStreamableHttpTransport embeddedTransport(EmbeddedServer embeddedServer, McpServerConfiguration mcpServerConfiguration) {
        return HttpClientStreamableHttpTransport
            .builder(embeddedServer.getURL().toString() + mcpServerConfiguration.getEndpoint())
            .build();
    }
}
