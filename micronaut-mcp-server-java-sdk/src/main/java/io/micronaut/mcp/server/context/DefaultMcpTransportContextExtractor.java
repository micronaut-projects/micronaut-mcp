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
import io.micronaut.http.HttpRequest;
import io.micronaut.http.server.util.HttpHostResolver;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.server.McpTransportContextExtractor;
import jakarta.inject.Singleton;

/**
 * Extracts a {@link MicronautMcpTransportContext} that reads its values from the HTTP request when they are asked for.
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
        return new HttpRequestMcpTransportContext(request, hostResolver, localeResolver);
    }
}
