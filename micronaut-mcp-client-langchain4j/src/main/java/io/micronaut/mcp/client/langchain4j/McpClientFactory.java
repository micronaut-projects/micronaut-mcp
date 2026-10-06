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
package io.micronaut.mcp.client.langchain4j;

import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.McpClientListener;
import dev.langchain4j.mcp.client.logging.McpLogMessageHandler;
import dev.langchain4j.mcp.client.transport.McpTransport;
import io.micronaut.context.BeanContext;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.EachBean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Prototype;
import io.micronaut.core.annotation.Internal;
import io.micronaut.inject.qualifiers.Qualifiers;
import io.micronaut.mcp.client.langchain4j.http.MicronautHttpClientTransports;
import io.micronaut.mcp.conf.client.McpClientConnectionConfiguration;
import io.micronaut.mcp.conf.client.McpClientHttpConfiguration;
import io.micronaut.mcp.conf.client.McpHttpClientType;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Creates an {@link McpClient} for each connection, over the transport of the connection.
 */
@Internal
@Factory
final class McpClientFactory {

    @EachBean(McpClientConnectionConfiguration.class)
    @Prototype
    DefaultMcpClient.Builder crateMcpClientBuilder(McpClientConnectionConfiguration configuration,
                                                   BeanContext beanContext,
                                                   List<McpClientListener> listeners,
                                                   @Nullable McpLogMessageHandler logMessageHandler,
                                                   @Nullable MicronautHttpClientTransports micronautTransports) {
        DefaultMcpClient.Builder builder = new DefaultMcpClient.Builder()
            .transport(transport(configuration, beanContext, micronautTransports))
            // The key identifies the client, for example in tool name mappers and filters
            .key(configuration.getName())
            .addListeners(listeners);
        if (configuration.getInitializationTimeout() != null) {
            builder.initializationTimeout(configuration.getInitializationTimeout());
        }
        if (configuration.getRequestTimeout() != null) {
            builder.toolExecutionTimeout(configuration.getRequestTimeout())
                .resourcesTimeout(configuration.getRequestTimeout())
                .promptsTimeout(configuration.getRequestTimeout());
        }
        if (logMessageHandler != null) {
            builder.logHandler(logMessageHandler);
        }
        return builder;
    }

    private static McpTransport transport(McpClientConnectionConfiguration configuration,
                                          BeanContext beanContext,
                                          @Nullable MicronautHttpClientTransports micronautTransports) {
        if (configuration instanceof McpClientHttpConfiguration http && http.getHttpClient() == McpHttpClientType.MICRONAUT) {
            if (micronautTransports == null) {
                throw new IllegalStateException("The MCP connection " + http.getName() + " uses the Micronaut HTTP client, add the micronaut-http-client dependency");
            }
            return micronautTransports.create(http);
        }
        return beanContext.getBean(McpTransport.class, Qualifiers.byName(configuration.getName()));
    }

    @EachBean(DefaultMcpClient.Builder.class)
    @Bean(preDestroy = "close")
    @Singleton
    McpClient createMcpClient(DefaultMcpClient.Builder builder) {
        return builder.build();
    }
}
