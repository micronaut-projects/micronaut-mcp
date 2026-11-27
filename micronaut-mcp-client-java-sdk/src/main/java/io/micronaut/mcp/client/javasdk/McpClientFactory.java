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

import io.micronaut.context.BeanContext;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.EachBean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Parameter;
import io.micronaut.context.annotation.Prototype;
import io.micronaut.core.annotation.Internal;
import io.micronaut.inject.qualifiers.Qualifiers;
import io.micronaut.mcp.conf.client.McpClientConnectionConfiguration;
import io.micronaut.mcp.conf.client.McpClientHeadersProvider;
import io.micronaut.mcp.conf.client.McpClientHttpConfiguration;
import io.micronaut.mcp.conf.client.McpClientRequestHeaders;
import io.micronaut.mcp.conf.client.McpClientStdioConfiguration;
import io.micronaut.mcp.conf.client.McpHttpClientType;
import io.micronaut.context.exceptions.DisabledBeanException;
import io.micronaut.scheduling.TaskExecutors;
import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.schema.JsonSchemaValidator;
import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.net.URI;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * Creates the transport of each connection, Streamable HTTP or STDIO, and a synchronous or an asynchronous MCP client
 * for each connection, which call the handler beans of the application. A connection has either kind of client, not
 * both, since each client opens its own session over its own transport. The clients are initialized when they are
 * created, and a client that fails to initialize is closed, with its transport, so that the next attempt starts afresh.
 */
@Factory
@Internal
final class McpClientFactory {
    private static final Logger LOG = LoggerFactory.getLogger(McpClientFactory.class);
    private final McpJsonMapper mcpJsonMapper;
    private final BeanContext beanContext;
    private final @Nullable McpSamplingHandler samplingHandler;
    private final @Nullable McpElicitationHandler elicitationHandler;
    private final List<McpLoggingHandler> loggingHandlers;
    private final List<McpProgressHandler> progressHandlers;
    private final List<McpListChangedListener> listChangedListeners;
    private final List<McpClientHeadersProvider> headersProviders;
    private final @Nullable MicronautHttpClientTransports micronautTransports;
    private final McpConnectionClients connectionClients;
    private final Scheduler blockingScheduler;

    @SuppressWarnings({"java:S107", "ParameterNumber"})
    McpClientFactory(McpJsonMapper mcpJsonMapper,
                     BeanContext beanContext,
                     @Nullable McpSamplingHandler samplingHandler,
                     @Nullable McpElicitationHandler elicitationHandler,
                     List<McpLoggingHandler> loggingHandlers,
                     List<McpProgressHandler> progressHandlers,
                     List<McpListChangedListener> listChangedListeners,
                     List<McpClientHeadersProvider> headersProviders,
                     @Nullable MicronautHttpClientTransports micronautTransports,
                     McpConnectionClients connectionClients,
                     @Named(TaskExecutors.BLOCKING) ExecutorService blockingExecutor) {
        this.micronautTransports = micronautTransports;
        this.headersProviders = headersProviders;
        this.mcpJsonMapper = mcpJsonMapper;
        this.beanContext = beanContext;
        this.samplingHandler = samplingHandler;
        this.elicitationHandler = elicitationHandler;
        this.loggingHandlers = loggingHandlers;
        this.progressHandlers = progressHandlers;
        this.listChangedListeners = listChangedListeners;
        this.connectionClients = connectionClients;
        // The handlers of the application may block, so the asynchronous client calls them on the blocking executor
        this.blockingScheduler = Schedulers.fromExecutorService(blockingExecutor, "mcp-client");
    }

    @EachBean(McpClientHttpConfiguration.class)
    @Prototype
    HttpClientStreamableHttpTransport.Builder transportBuilder(McpClientHttpConfiguration configuration) {
        if (configuration.getHttpClient() == McpHttpClientType.MICRONAUT) {
            throw new DisabledBeanException("The connection " + configuration.getName() + " uses the Micronaut HTTP client");
        }
        URI url = configuration.getUrl();
        HttpClientStreamableHttpTransport.Builder builder = HttpClientStreamableHttpTransport
            .builder(url.toString())
            .jsonMapper(mcpJsonMapper);
        // The SDK resolves its endpoint against the base URI. Supply an absolute URI to retain
        // the configured authority, raw path and query, including a path starting with two slashes.
        if (!url.getRawPath().isEmpty()) {
            builder.endpoint(url.toASCIIString());
        } else if (url.getRawQuery() != null) {
            builder.endpoint("/mcp?" + url.getRawQuery());
        }
        if (!configuration.getHeaders().isEmpty()) {
            HttpRequest.Builder request = HttpRequest.newBuilder();
            configuration.getHeaders().forEach(request::header);
            builder.requestBuilder(request);
        }
        // The synchronous client computes the headers in its transport context provider, on the thread that calls it. For
        // the asynchronous client, they are computed here, in the Micronaut context propagated by the subscriber
        builder.asyncHttpRequestCustomizer((request, method, endpoint, body, context) -> Mono.deferContextual(reactorContext -> {
            TransportHeaders.headers(configuration, headersProviders, context, reactorContext).forEach(request::setHeader);
            return Mono.just(request);
        }));
        return builder;
    }

    @EachBean(HttpClientStreamableHttpTransport.Builder.class)
    @Prototype
    HttpClientStreamableHttpTransport transport(HttpClientStreamableHttpTransport.Builder builder) {
        return builder.build();
    }

    @EachBean(McpClientStdioConfiguration.class)
    @Prototype
    StdioClientTransport stdioTransport(McpClientStdioConfiguration configuration) {
        List<String> command = configuration.getCommand();
        if (command.isEmpty()) {
            throw new IllegalStateException("The command of the MCP STDIO connection " + configuration.getName() + " is empty");
        }
        ServerParameters parameters = ServerParameters.builder(command.getFirst())
            .args(command.subList(1, command.size()))
            .env(configuration.getEnvironment())
            .build();
        return new StdioClientTransport(parameters, mcpJsonMapper);
    }

    @EachBean(McpClientConnectionConfiguration.class)
    @Prototype
    McpClient.SyncSpec mcpClientSyncSpec(@NonNull McpClientConnectionConfiguration configuration,
                                         @NonNull JsonSchemaValidator jsonSchemaValidator) {
        String name = configuration.getName();
        McpClient.SyncSpec spec = McpClient.sync(transport(configuration))
            .jsonSchemaValidator(jsonSchemaValidator)
            .capabilities(capabilities());
        if (configuration instanceof McpClientHttpConfiguration http && McpClientRequestHeaders.isDynamic(http, headersProviders)) {
            spec.transportContextProvider(() -> McpClientTransportHeaders.transportContext(McpClientRequestHeaders.headers(http, headersProviders)));
        } else {
            spec.toolsChangeConsumer(tools -> connectionClients.toolsChanged(name, tools));
        }
        Duration requestTimeout = requestTimeout(configuration);
        if (requestTimeout != null) {
            spec.requestTimeout(requestTimeout);
        }
        if (configuration.getInitializationTimeout() != null) {
            spec.initializationTimeout(configuration.getInitializationTimeout());
        }
        McpSamplingHandler sampling = samplingHandler;
        if (sampling != null) {
            spec.sampling(request -> sampling.sample(name, request));
        }
        McpElicitationHandler elicitation = elicitationHandler;
        if (elicitation != null) {
            spec.elicitation(request -> elicitation.elicit(name, request));
        }
        for (McpLoggingHandler handler : loggingHandlers) {
            spec.loggingConsumer(notification -> handler.log(name, notification));
        }
        for (McpProgressHandler handler : progressHandlers) {
            spec.progressConsumer(notification -> handler.progress(name, notification));
        }
        for (McpListChangedListener listener : listChangedListeners) {
            spec.toolsChangeConsumer(tools -> listener.toolsChanged(name, tools));
            spec.promptsChangeConsumer(prompts -> listener.promptsChanged(name, prompts));
            spec.resourcesChangeConsumer(resources -> listener.resourcesChanged(name, resources));
        }
        return spec;
    }

    @EachBean(McpClient.SyncSpec.class)
    @Bean(preDestroy = "closeGracefully")
    @Singleton
    McpSyncClient mcpSyncClient(McpClient.SyncSpec spec,
                                @Parameter @Nullable McpClientConnectionConfiguration configuration) {
        if (configuration == null) {
            // Application-provided specs keep the SDK's lazy initialization.
            return spec.build();
        }
        String name = configuration.getName();
        var claim = connectionClients.claim(name, McpSyncClient.class);
        McpSyncClient client = null;
        try {
            client = spec.build();
            if (Schedulers.isInNonBlockingThread()) {
                // An event loop must not block, so the client initializes on the blocking executor instead, and a
                // failure is cleaned up there
                McpSyncClient created = client;
                blockingScheduler.schedule(() -> {
                    try {
                        created.initialize();
                    } catch (RuntimeException e) {
                        initializationFailed(name, McpSyncClient.class, created, claim, e);
                    }
                });
            } else {
                client.initialize();
            }
            return client;
        } catch (RuntimeException e) {
            if (client != null) {
                closeQuietly(name, client::closeGracefully);
            }
            connectionClients.release(name, claim);
            throw e;
        }
    }

    @EachBean(McpClientConnectionConfiguration.class)
    @Prototype
    McpClient.AsyncSpec mcpAysncSpec(@NonNull McpClientConnectionConfiguration configuration,
                                     @NonNull JsonSchemaValidator jsonSchemaValidator) {
        String name = configuration.getName();
        McpClient.AsyncSpec spec = McpClient.async(transport(configuration))
            .jsonSchemaValidator(jsonSchemaValidator)
            .capabilities(capabilities());
        Duration requestTimeout = requestTimeout(configuration);
        if (requestTimeout != null) {
            spec.requestTimeout(requestTimeout);
        }
        if (configuration.getInitializationTimeout() != null) {
            spec.initializationTimeout(configuration.getInitializationTimeout());
        }
        // The handlers are synchronous, so the asynchronous client calls them on threads that may block
        McpSamplingHandler sampling = samplingHandler;
        if (sampling != null) {
            spec.sampling(request -> Mono.fromCallable(() -> sampling.sample(name, request)).subscribeOn(blockingScheduler));
        }
        McpElicitationHandler elicitation = elicitationHandler;
        if (elicitation != null) {
            spec.elicitation(request -> Mono.fromCallable(() -> elicitation.elicit(name, request)).subscribeOn(blockingScheduler));
        }
        for (McpLoggingHandler handler : loggingHandlers) {
            spec.loggingConsumer(notification -> Mono.<Void>fromRunnable(() -> handler.log(name, notification)).subscribeOn(blockingScheduler));
        }
        for (McpProgressHandler handler : progressHandlers) {
            spec.progressConsumer(notification -> Mono.<Void>fromRunnable(() -> handler.progress(name, notification)).subscribeOn(blockingScheduler));
        }
        for (McpListChangedListener listener : listChangedListeners) {
            spec.toolsChangeConsumer(tools -> Mono.<Void>fromRunnable(() -> listener.toolsChanged(name, tools)).subscribeOn(blockingScheduler));
            spec.promptsChangeConsumer(prompts -> Mono.<Void>fromRunnable(() -> listener.promptsChanged(name, prompts)).subscribeOn(blockingScheduler));
            spec.resourcesChangeConsumer(resources -> Mono.<Void>fromRunnable(() -> listener.resourcesChanged(name, resources)).subscribeOn(blockingScheduler));
        }
        return spec;
    }

    // Closed by McpAsyncClientCloser
    @EachBean(McpClient.AsyncSpec.class)
    @Singleton
    McpAsyncClient mcpAsyncClient(McpClient.AsyncSpec spec,
                                  @Parameter @Nullable McpClientConnectionConfiguration configuration) {
        if (configuration == null) {
            // Application-provided specs keep the SDK's lazy initialization.
            return spec.build();
        }
        String name = configuration.getName();
        var claim = connectionClients.claim(name, McpAsyncClient.class);
        McpAsyncClient client = null;
        try {
            client = spec.build();
            if (Schedulers.isInNonBlockingThread()) {
                // An event loop must not block, so the client initializes without waiting, and a failure is cleaned up on
                // the blocking executor, as closing the client waits for its transport
                McpAsyncClient created = client;
                client.initialize().subscribe(result -> { },
                    e -> blockingScheduler.schedule(() -> initializationFailed(name, McpAsyncClient.class, created, claim, e)));
            } else {
                // Bounded by the initialization timeout of the client
                client.initialize().block();
            }
            return client;
        } catch (RuntimeException e) {
            if (client != null) {
                McpAsyncClient failed = client;
                closeQuietly(name, () -> McpAsyncClientCloser.close(failed));
            }
            connectionClients.release(name, claim);
            throw e;
        }
    }

    /**
     * Cleans up a client whose initialization failed after it was returned, as a failure while it is created is: the
     * client is destroyed, which closes it with its transport, such as the process of a STDIO server, and the connection
     * is released, so that the next request for the client creates it afresh.
     */
    private <T> void initializationFailed(String name, Class<T> type, T client, McpConnectionClients.Claim claim, Throwable error) {
        LOG.warn("The MCP client of the connection {} failed to initialize, it is created again the next time it is requested", name, error);
        try {
            // A fast failure can reach this worker before the factory returns. A qualified lookup waits until Micronaut
            // publishes the singleton, so destruction removes the failed registration as well as closing its transport.
            var registration = beanContext.getBeanRegistration(type, Qualifiers.byName(name));
            if (registration.getBean() == client) {
                beanContext.destroyBean(registration);
            }
        } catch (RuntimeException e) {
            LOG.warn("Failed to close the MCP client of the connection {}", name, e);
        } finally {
            connectionClients.release(name, claim);
        }
    }

    private static void closeQuietly(String name, Runnable close) {
        try {
            close.run();
        } catch (RuntimeException e) {
            LOG.warn("Failed to close the MCP client of the connection {}", name, e);
        }
    }

    private McpClientTransport transport(McpClientConnectionConfiguration configuration) {
        if (configuration instanceof McpClientHttpConfiguration http && http.getHttpClient() == McpHttpClientType.MICRONAUT) {
            if (micronautTransports == null) {
                throw new IllegalStateException("The MCP connection " + http.getName() + " uses the Micronaut HTTP client, add the micronaut-http-client dependency");
            }
            return micronautTransports.create(http);
        }
        return beanContext.getBean(McpClientTransport.class, Qualifiers.byName(configuration.getName()));
    }

    private McpSchema.ClientCapabilities capabilities() {
        McpSchema.ClientCapabilities.Builder capabilities = McpSchema.ClientCapabilities.builder();
        if (samplingHandler != null) {
            capabilities.sampling();
        }
        if (elicitationHandler != null) {
            capabilities.elicitation();
        }
        return capabilities.build();
    }

    private static @Nullable Duration requestTimeout(McpClientConnectionConfiguration configuration) {
        if (configuration.getRequestTimeout() != null) {
            return configuration.getRequestTimeout();
        }
        return configuration instanceof McpClientHttpConfiguration http ? http.getTimeout() : null;
    }
}
