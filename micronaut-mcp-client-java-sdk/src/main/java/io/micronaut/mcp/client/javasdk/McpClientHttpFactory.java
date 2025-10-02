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

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.EachBean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Parameter;
import io.micronaut.context.annotation.Prototype;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.mcp.conf.client.McpClientHttpConfiguration;
import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.json.schema.JsonSchemaValidator;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Singleton;

/**
 * Factory responsible for creating {@link McpSyncClient} and {@link McpAsyncClient}.
 */
@Factory
@Internal
final class McpClientHttpFactory {
    @EachBean(McpClientHttpConfiguration.class)
    @Prototype
    HttpClientStreamableHttpTransport transport(McpClientHttpConfiguration clientHttpConfiguration) {
        return HttpClientStreamableHttpTransport
            .builder(clientHttpConfiguration.getUrl().toString())
            .build();
    }

    @EachBean(HttpClientStreamableHttpTransport.class)
    @Prototype
    McpSchema.ClientCapabilities clientCapabilities(HttpClientStreamableHttpTransport transport) {
        return McpSchema.ClientCapabilities.builder().build();
    }

    @EachBean(HttpClientStreamableHttpTransport.class)
    @Bean(preDestroy = "close")
    @Singleton
    McpSyncClient mcpSyncClient(@NonNull HttpClientStreamableHttpTransport transport,
                                @NonNull JsonSchemaValidator jsonSchemaValidator,
                                @Parameter McpSchema.ClientCapabilities clientCapabilities,
                                @Nullable @Parameter McpClientHttpConfiguration config) {
        McpClient.SyncSpec builder = McpClient.sync(transport)
            .jsonSchemaValidator(jsonSchemaValidator)
            .capabilities(clientCapabilities);
        if (config != null && config.getTimeout() != null) {
            builder.requestTimeout(config.getTimeout());
        }
        return builder.build();
    }

    @EachBean(HttpClientStreamableHttpTransport.class)
    @Bean(preDestroy = "close")
    @Singleton
    McpAsyncClient mcpAysncClient(@NonNull HttpClientStreamableHttpTransport transport,
                                  @NonNull JsonSchemaValidator jsonSchemaValidator,
                                  @Parameter McpSchema.ClientCapabilities clientCapabilities,
                                  @Nullable @Parameter McpClientHttpConfiguration config) {
        McpClient.AsyncSpec builder = McpClient.async(transport)
            .jsonSchemaValidator(jsonSchemaValidator)
            .capabilities(clientCapabilities);
        if (config != null && config.getTimeout() != null) {
            builder.requestTimeout(config.getTimeout());
        }
        return builder.build();
    }
}
