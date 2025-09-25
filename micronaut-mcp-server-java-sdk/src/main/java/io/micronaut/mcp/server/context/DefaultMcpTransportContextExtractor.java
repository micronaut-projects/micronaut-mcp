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
package io.micronaut.mcp.server.context;

import io.micronaut.core.annotation.Internal;
import io.micronaut.core.util.LocaleResolver;
import io.micronaut.http.HttpAttributes;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.server.util.HttpHostResolver;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.server.McpTransportContextExtractor;
import io.modelcontextprotocol.spec.ProtocolVersions;
import jakarta.inject.Singleton;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

/**
 * Default implementation of {@link McpTransportContextExtractor}.
 */
@Internal
@Singleton
final class DefaultMcpTransportContextExtractor implements McpTransportContextExtractor<HttpRequest<?>> {
    private final HttpHostResolver hostResolver;
    private final LocaleResolver<HttpRequest<?>> localeResolver;

    DefaultMcpTransportContextExtractor(HttpHostResolver hostResolver,
                                        LocaleResolver<HttpRequest<?>> localeResolver) {
        this.hostResolver = hostResolver;
        this.localeResolver = localeResolver;
    }

    @Override
    public McpTransportContext extract(HttpRequest<?> request) {
        return new MicronautMcpTransportContextAdapter(McpTransportContext.create(metadata(request)));
    }

    private Map<String, Object> metadata(HttpRequest<?> request) {
        Map<String, Object> m = new HashMap<>(metadata(request.getHeaders()));
        m.put(HttpHeaders.HOST, hostResolver.resolve(request));
        localeResolver.resolve(request)
            .ifPresent(locale -> m.put(HttpHeaders.ACCEPT_LANGUAGE, locale));
        request.getAttribute(HttpAttributes.PRINCIPAL.toString(), Principal.class)
            .ifPresent(auth -> m.put(HttpAttributes.PRINCIPAL.toString(), auth));
        return m;
    }

    private Map<String, Object> metadata(HttpHeaders headers) {
        Map<String, Object> metadata = new HashMap<>(3);
        metadata.put(io.modelcontextprotocol.spec.HttpHeaders.PROTOCOL_VERSION,
            headers.get(io.modelcontextprotocol.spec.HttpHeaders.PROTOCOL_VERSION, String.class)
                .orElse(ProtocolVersions.MCP_2025_03_26));
        headers.get(io.modelcontextprotocol.spec.HttpHeaders.MCP_SESSION_ID, String.class)
            .ifPresent(v -> metadata.put(io.modelcontextprotocol.spec.HttpHeaders.MCP_SESSION_ID, v));
        headers.get(io.modelcontextprotocol.spec.HttpHeaders.LAST_EVENT_ID, String.class)
            .ifPresent(v -> metadata.put(io.modelcontextprotocol.spec.HttpHeaders.LAST_EVENT_ID, v));
        return metadata;
    }
}
