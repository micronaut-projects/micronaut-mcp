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
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.server.util.HttpHostResolver;
import io.modelcontextprotocol.spec.ProtocolVersions;
import org.jspecify.annotations.Nullable;

import java.security.Principal;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A {@link MicronautMcpTransportContext} backed by the HTTP request. Each value is read from the request when it is first
 * asked for, so requests whose handlers never read the host, the locale or the principal do not pay for resolving them.
 */
@Internal
final class HttpRequestMcpTransportContext implements MicronautMcpTransportContext {
    private static final Object UNRESOLVED = new Object();

    private final HttpRequest<?> request;
    private final HttpHostResolver hostResolver;
    private final LocaleResolver<HttpRequest<?>> localeResolver;
    private final AtomicReference<Object> host = new AtomicReference<>(UNRESOLVED);
    private final AtomicReference<Object> locale = new AtomicReference<>(UNRESOLVED);

    HttpRequestMcpTransportContext(HttpRequest<?> request,
                                   HttpHostResolver hostResolver,
                                   LocaleResolver<HttpRequest<?>> localeResolver) {
        this.request = request;
        this.hostResolver = hostResolver;
        this.localeResolver = localeResolver;
    }

    @Override
    public @Nullable Object get(String key) {
        return switch (key) {
            case io.modelcontextprotocol.spec.HttpHeaders.PROTOCOL_VERSION -> protocolVersion();
            case io.modelcontextprotocol.spec.HttpHeaders.MCP_SESSION_ID -> sessionId();
            case io.modelcontextprotocol.spec.HttpHeaders.LAST_EVENT_ID -> lastEventId();
            case HttpHeaders.HOST -> host();
            case HttpHeaders.ACCEPT_LANGUAGE -> locale();
            case MicronautMcpTransportContextAdapter.PRINCIPAL_KEY -> principal();
            default -> null;
        };
    }

    @Override
    public @Nullable Locale locale() {
        Object value = locale.get();
        if (value == UNRESOLVED) {
            value = localeResolver.resolve(request).orElse(null);
            locale.set(value);
        }
        return (Locale) value;
    }

    @Override
    public @Nullable String host() {
        Object value = host.get();
        if (value == UNRESOLVED) {
            value = hostResolver.resolve(request);
            host.set(value);
        }
        return (String) value;
    }

    @Override
    public @Nullable Principal principal() {
        return request.getUserPrincipal().orElse(null);
    }

    @Override
    public @Nullable String lastEventId() {
        return request.getHeaders().get(io.modelcontextprotocol.spec.HttpHeaders.LAST_EVENT_ID);
    }

    @Override
    public @Nullable String sessionId() {
        return request.getHeaders().get(io.modelcontextprotocol.spec.HttpHeaders.MCP_SESSION_ID);
    }

    @Override
    public String protocolVersion() {
        String protocolVersion = request.getHeaders().get(io.modelcontextprotocol.spec.HttpHeaders.PROTOCOL_VERSION);
        return protocolVersion != null ? protocolVersion : ProtocolVersions.MCP_2025_03_26;
    }
}
