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
package io.micronaut.mcp.conf.client;

import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.Internal;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.context.ServerRequestContext;
import jakarta.inject.Singleton;
import org.jspecify.annotations.NonNull;

import java.util.Map;

/**
 * Sends the {@code Authorization} header of the HTTP request the server is handling to the MCP servers of the
 * connections that {@link McpClientHttpConfiguration#isPropagateAuthorization() propagate} it. Only bearer tokens are
 * propagated, unless the connection {@link McpClientHttpConfiguration#isPropagateAnyAuthorizationScheme() propagates any
 * scheme}.
 *
 * <p>This passes through a token the MCP server did not issue for itself, which the
 * <a href="https://modelcontextprotocol.io/specification/2025-11-25/basic/authorization">MCP authorization specification</a>
 * forbids: a server that follows it rejects the token, and one that skips the audience check becomes a confused deputy.
 * Only use it with trusted servers that share the audience of the token. Otherwise exchange the token, for example with
 * <a href="https://datatracker.ietf.org/doc/html/rfc8693">OAuth 2.0 Token Exchange</a>, in a custom
 * {@link McpClientHeadersProvider}.</p>
 */
@Singleton
@Internal
@Requires(classes = ServerRequestContext.class)
final class AuthorizationPropagationHeadersProvider implements McpClientHeadersProvider {
    private static final String BEARER = "Bearer ";

    @Override
    public boolean supports(@NonNull McpClientHttpConfiguration connection) {
        return connection.isPropagateAuthorization();
    }

    @Override
    public @NonNull Map<String, String> headers(@NonNull McpClientHttpConfiguration connection) {
        return ServerRequestContext.currentRequest()
            .flatMap(request -> request.getHeaders().getAuthorization())
            .filter(authorization -> connection.isPropagateAnyAuthorizationScheme() || authorization.regionMatches(true, 0, BEARER, 0, BEARER.length()))
            .map(authorization -> Map.of(HttpHeaders.AUTHORIZATION, authorization))
            .orElse(Map.of());
    }
}
