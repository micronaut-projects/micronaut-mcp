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
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpAttributes;
import io.micronaut.http.HttpHeaders;
import io.modelcontextprotocol.common.McpTransportContext;

import java.security.Principal;
import java.util.Locale;

@Internal
final class MicronautMcpTransportContextAdapter implements MicronautMcpTransportContext {
    private final McpTransportContext delegate;

    MicronautMcpTransportContextAdapter(McpTransportContext context) {
        this.delegate = context;
    }

    @Override
    public Object get(String key) {
        return delegate.get(key);
    }

    @Nullable
    @Override
    public Locale locale() {
        Object obj = get(HttpHeaders.ACCEPT_LANGUAGE);
        if (obj instanceof Locale locale) {
            return locale;
        }
        return null;
    }

    @Nullable
    @Override
    public String host() {
        return getString(HttpHeaders.HOST);
    }

    @Nullable
    @Override
    public Principal principal() {
        Object obj = get(HttpAttributes.PRINCIPAL.toString());
        if (obj instanceof Principal principal) {
            return principal;
        }
        return null;
    }

    @Nullable
    @Override
    public String lastEventId() {
        return getString(io.modelcontextprotocol.spec.HttpHeaders.LAST_EVENT_ID);
    }

    @Nullable
    @Override
    public String sessionId() {
        return getString(io.modelcontextprotocol.spec.HttpHeaders.MCP_SESSION_ID);
    }

    @Nullable
    @Override
    public String protocolVersion() {
        return getString(io.modelcontextprotocol.spec.HttpHeaders.PROTOCOL_VERSION);
    }

    private String getString(String key) {
        Object obj = get(key);
        if (obj instanceof String str) {
            return str;
        }
        return null;
    }
}
