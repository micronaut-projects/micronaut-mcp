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
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.EachBean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Parameter;
import io.micronaut.context.annotation.Prototype;
import io.micronaut.core.annotation.Internal;
import io.micronaut.mcp.conf.client.McpClientConnectionConfiguration;
import io.micronaut.mcp.conf.client.McpClientHeadersProvider;
import io.micronaut.mcp.conf.client.McpClientHttpConfiguration;
import io.micronaut.mcp.conf.client.McpClientRequestHeaders;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.List;

/**
 * Creates an {@link McpClient} for each transport, applying the named connection settings when present.
 */
@Internal
@Factory
final class McpClientFactory {
    private final List<McpClientHeadersProvider> headersProviders;

    McpClientFactory() {
        this(List.of());
    }

    @Inject
    McpClientFactory(List<McpClientHeadersProvider> headersProviders) {
        this.headersProviders = headersProviders;
    }

    @EachBean(McpTransport.class)
    @Prototype
    DefaultMcpClient.Builder crateMcpClientBuilder(McpTransport transport,
                                                   @Parameter @Nullable McpClientConnectionConfiguration configuration,
                                                   List<McpClientListener> listeners,
                                                   @Nullable McpLogMessageHandler logMessageHandler) {
        DefaultMcpClient.Builder builder = new TransportOwningBuilder(transport).addListeners(listeners);
        if (logMessageHandler != null) {
            builder.logHandler(logMessageHandler);
        }
        if (configuration == null) {
            return builder;
        }
        builder.key(configuration.getName()).autoHealthCheck(configuration.isAutoHealthCheck());
        Duration initializationTimeout = configuration.getInitializationTimeout();
        if (initializationTimeout != null) {
            builder.initializationTimeout(initializationTimeout);
        }
        Duration requestTimeout = configuration.getRequestTimeout();
        if (requestTimeout != null) {
            builder.toolExecutionTimeout(requestTimeout)
                .resourcesTimeout(requestTimeout)
                .promptsTimeout(requestTimeout)
                .pingTimeout(requestTimeout);
        }
        if (configuration instanceof McpClientHttpConfiguration http && McpClientRequestHeaders.isDynamic(http, headersProviders)) {
            // The tools, resources and prompts a server lists may depend on the credentials of the caller, which the
            // headers of each request carry, so they are listed for each caller instead of being shared by every one
            builder.cacheToolList(false).cacheResourceList(false).cachePromptList(false);
        }
        return builder;
    }

    @EachBean(DefaultMcpClient.Builder.class)
    @Bean(preDestroy = "close")
    @Singleton
    McpClient createMcpClient(DefaultMcpClient.Builder builder) {
        return builder.build();
    }

    /**
     * The SDK client closes its transport after a successful build. If initialization fails, close the same
     * transport the builder used, including one installed by a bean creation listener.
     */
    private static final class TransportOwningBuilder extends DefaultMcpClient.Builder {
        private McpTransport transport;

        TransportOwningBuilder(McpTransport transport) {
            this.transport = transport;
            super.transport(transport);
        }

        @Override
        public DefaultMcpClient.Builder transport(McpTransport transport) {
            this.transport = transport;
            return super.transport(transport);
        }

        @Override
        public DefaultMcpClient build() {
            try {
                return super.build();
            } catch (RuntimeException failure) {
                try {
                    transport.close();
                } catch (Exception closeFailure) {
                    if (closeFailure != failure) {
                        failure.addSuppressed(closeFailure);
                    }
                }
                throw failure;
            }
        }
    }
}
