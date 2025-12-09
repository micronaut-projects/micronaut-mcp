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

import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.Internal;
import org.jspecify.annotations.NonNull;
import io.micronaut.mcp.conf.server.McpServerConfiguration;
import io.micronaut.runtime.server.EmbeddedServer;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import java.net.URI;

@Requires(classes = { EmbeddedServer.class, McpServerConfiguration.class })
@Requires(beans = { EmbeddedServer.class, McpServerConfiguration.class })
@Internal
@Factory
final class McpClientHttpConfigurationEmbeddedServerFactory {
    private static final String EMBEDDED_SERVER = "embeddedServer";

    @Named(EMBEDDED_SERVER)
    @Singleton
    McpClientHttpConfiguration embeddedTransport(@NonNull EmbeddedServer embeddedServer,
                                                  @NonNull McpServerConfiguration mcpServerConfiguration) {
        return McpClientHttpConfiguration.of(EMBEDDED_SERVER,
            URI.create(embeddedServer.getURL().toString() + mcpServerConfiguration.getEndpoint()));
    }
}
