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
package io.micronaut.mcp.client.http;

import io.micronaut.context.BeanContext;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.AnnotationMetadata;
import io.micronaut.core.annotation.Internal;
import io.micronaut.http.client.DefaultHttpClientConfiguration;
import io.micronaut.http.client.HttpClientConfiguration;
import io.micronaut.http.client.LoadBalancer;
import io.micronaut.http.client.ServiceHttpClientConfiguration;
import io.micronaut.http.client.StreamingHttpClient;
import io.micronaut.http.client.StreamingHttpClientRegistry;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.inject.annotation.MutableAnnotationMetadata;
import io.micronaut.inject.qualifiers.Qualifiers;
import io.micronaut.mcp.conf.client.McpClientHeadersProvider;
import io.micronaut.mcp.conf.client.McpClientHttpConfiguration;
import io.micronaut.scheduling.TaskExecutors;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

/**
 * Creates the exchanges of the HTTP connections that use the Micronaut HTTP client, over one Micronaut HTTP client for
 * each connection.
 *
 * <p>With a {@link McpClientHttpConfiguration#getServiceId() service id}, the {@code micronaut.http.services} entry of
 * that id configures the client; when the entry has no URL, the requests go to the host of the URL of the connection.
 * Without one, the default client configuration applies, with the request timeout of the connection, or its timeout, as
 * the read timeout.</p>
 */
@Singleton
@Internal
@Requires(classes = StreamingHttpClient.class)
// Only http-client-core can be on the classpath, without the Micronaut HTTP client that provides the registry
@Requires(beans = StreamingHttpClientRegistry.class)
public final class MicronautMcpHttpClients {
    private final StreamingHttpClientRegistry<?> registry;
    private final BeanContext beanContext;
    private final List<McpClientHeadersProvider> headersProviders;
    private final Scheduler scheduler;
    private final Map<String, Endpoint> endpoints = new ConcurrentHashMap<>();

    MicronautMcpHttpClients(StreamingHttpClientRegistry<?> registry,
                            BeanContext beanContext,
                            List<McpClientHeadersProvider> headersProviders,
                            @Named(TaskExecutors.BLOCKING) ExecutorService blockingExecutor) {
        this.registry = registry;
        this.beanContext = beanContext;
        this.headersProviders = headersProviders;
        // The messages are parsed, and the handlers of the clients called, off the event loop
        this.scheduler = Schedulers.fromExecutorService(blockingExecutor, "mcp-client-http");
    }

    /**
     * @param configuration The connection
     * @param defaultPath The path used by the client adapter for a URL without a path
     * @return A new exchange with the server of the connection
     */
    public MicronautMcpHttpExchange exchange(McpClientHttpConfiguration configuration, String defaultPath) {
        Endpoint endpoint = endpoints.computeIfAbsent(configuration.getName(), name -> endpoint(configuration));
        String uri = endpoint.origin() + path(configuration.getUrl(), defaultPath);
        return new MicronautMcpHttpExchange(endpoint.client(), uri, configuration, headersProviders, scheduler);
    }

    private Endpoint endpoint(McpClientHttpConfiguration configuration) {
        URI url = configuration.getUrl();
        URI origin = URI.create(url.getScheme() + "://" + url.getRawAuthority());
        String serviceId = configuration.getServiceId();
        if (serviceId != null) {
            boolean serviceHasUrls = beanContext.findBean(ServiceHttpClientConfiguration.class, Qualifiers.byName(serviceId))
                .map(service -> !service.getUrls().isEmpty()).orElse(false);
            // The client ID selects service configuration and filters. An absolute request URI bypasses load balancing
            // when the connection supplies the origin.
            MutableAnnotationMetadata metadata = new MutableAnnotationMetadata();
            metadata.addDeclaredAnnotation(Client.class.getName(), Map.of(AnnotationMetadata.VALUE_MEMBER, serviceId));
            return new Endpoint(registry.getStreamingHttpClient(metadata), serviceHasUrls ? "" : origin.toString());
        }
        HttpClientConfiguration clientConfiguration = new ConnectionHttpClientConfiguration(defaultConfiguration());
        // MCP does not use redirects, and following one could send the token of a user to another server
        clientConfiguration.setFollowRedirects(false);
        Duration timeout = configuration.getRequestTimeout() != null ? configuration.getRequestTimeout() : configuration.getTimeout();
        if (timeout != null) {
            clientConfiguration.setReadTimeout(timeout);
        }
        return new Endpoint(registry.resolveStreamingHttpClient(null, LoadBalancer.fixed(origin), clientConfiguration, beanContext), "");
    }

    private static String path(URI url, String defaultPath) {
        String path = url.getRawPath() == null || url.getRawPath().isEmpty() ? defaultPath : url.getRawPath();
        return path + (url.getRawQuery() != null ? "?" + url.getRawQuery() : "");
    }

    private HttpClientConfiguration defaultConfiguration() {
        return beanContext.findBean(DefaultHttpClientConfiguration.class)
            .<HttpClientConfiguration>map(c -> c)
            .orElseGet(DefaultHttpClientConfiguration::new);
    }

    private record Endpoint(StreamingHttpClient client, String origin) {
    }

    /**
     * A copy of the default client configuration, with the timeout of a connection.
     */
    private static final class ConnectionHttpClientConfiguration extends HttpClientConfiguration {
        private final HttpClientConfiguration defaults;

        ConnectionHttpClientConfiguration(HttpClientConfiguration defaults) {
            super(defaults);
            this.defaults = defaults;
        }

        @Override
        public ConnectionPoolConfiguration getConnectionPoolConfiguration() {
            return defaults.getConnectionPoolConfiguration();
        }

        @Override
        public @Nullable WebSocketCompressionConfiguration getWebSocketCompressionConfiguration() {
            return defaults.getWebSocketCompressionConfiguration();
        }

        @Override
        public Http2ClientConfiguration getHttp2Configuration() {
            return defaults.getHttp2Configuration();
        }
    }
}
