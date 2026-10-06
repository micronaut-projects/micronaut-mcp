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
package io.micronaut.mcp.client.langchain4j.http;

import dev.langchain4j.mcp.client.transport.http.StreamableHttpMcpTransport;
import io.micronaut.context.annotation.EachBean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Prototype;
import io.micronaut.core.annotation.Internal;
import dev.langchain4j.mcp.client.McpHeadersSupplier;
import io.micronaut.mcp.conf.client.McpClientHeadersProvider;
import io.micronaut.mcp.conf.client.McpClientHttpConfiguration;
import io.micronaut.mcp.conf.client.McpClientRequestHeaders;
import io.micronaut.mcp.conf.client.McpHttpClientType;
import io.micronaut.context.exceptions.DisabledBeanException;
import jakarta.inject.Singleton;

import java.util.List;

@Factory
@Internal
final class StreamableHttpMcpTransportFactory {
    private final List<McpClientHeadersProvider> headersProviders;

    StreamableHttpMcpTransportFactory(List<McpClientHeadersProvider> headersProviders) {
        this.headersProviders = headersProviders;
    }

    @EachBean(McpClientHttpConfiguration.class)
    @Prototype
    StreamableHttpMcpTransport.Builder createStreamableHttpMcpTransportBuilder(McpClientHttpConfiguration config) {
        if (config.getHttpClient() == McpHttpClientType.MICRONAUT) {
            throw new DisabledBeanException("The connection " + config.getName() + " uses the Micronaut HTTP client");
        }
        StreamableHttpMcpTransport.Builder builder = new StreamableHttpMcpTransport.Builder()
            .url(config.getUrl().toString());
        if (config.getTimeout() != null) {
            builder.timeout(config.getTimeout());
        }
        builder.logRequests(config.isLogRequests());
        builder.logResponses(config.isLogResponses());
        if (McpClientRequestHeaders.isDynamic(config, headersProviders)) {
            // Computed for each request, on the thread that calls the client
            builder.customHeaders((McpHeadersSupplier) context -> McpClientRequestHeaders.headers(config, headersProviders));
        } else if (!config.getHeaders().isEmpty()) {
            builder.customHeaders(config.getHeaders());
        }
        return builder;
    }

    @EachBean(StreamableHttpMcpTransport.Builder.class)
    @Singleton
    StreamableHttpMcpTransport createStreamableHttpMcpTransport(StreamableHttpMcpTransport.Builder builder) {
        return builder.build();
    }
}
