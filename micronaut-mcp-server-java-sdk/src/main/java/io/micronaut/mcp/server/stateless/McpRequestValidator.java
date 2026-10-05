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
package io.micronaut.mcp.server.stateless;

import io.micronaut.core.annotation.Internal;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.mcp.conf.server.TransportSecurityConfiguration;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpStatelessServerTransport;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validates the {@code Origin} and {@code MCP-Protocol-Version} headers of requests to the MCP endpoint.
 *
 * @see <a href="https://modelcontextprotocol.io/specification/2025-11-25/basic/transports#security-warning">Streamable HTTP security</a>
 * @see <a href="https://modelcontextprotocol.io/specification/2025-11-25/basic/transports#protocol-version-header">Protocol version header</a>
 */
@Singleton
@Internal
final class McpRequestValidator {
    private static final String WILDCARD = "*";
    private static final Set<String> LOOPBACK_HOSTS = Set.of("localhost", "127.0.0.1", "[::1]", "::1");

    private final boolean enabled;
    private final boolean anyOrigin;
    private final Set<String> allowedOrigins;
    private final List<String> protocolVersions;

    McpRequestValidator(TransportSecurityConfiguration configuration, McpStatelessServerTransport transport) {
        this.enabled = configuration.isEnabled();
        this.anyOrigin = configuration.getAllowedOrigins().contains(WILDCARD);
        this.allowedOrigins = configuration.getAllowedOrigins().stream()
            .map(McpRequestValidator::normalize)
            .collect(Collectors.toUnmodifiableSet());
        this.protocolVersions = transport.protocolVersions();
    }

    /**
     * @param request The request
     * @return The response rejecting the request, or {@code null} when it is valid
     */
    @Nullable HttpResponse<?> reject(HttpRequest<?> request) {
        if (!enabled) {
            return null;
        }
        HttpHeaders headers = request.getHeaders();
        String origin = headers.get(HttpHeaders.ORIGIN);
        if (origin != null && !isAllowed(origin)) {
            return error(HttpStatus.FORBIDDEN, "Origin not allowed: " + origin);
        }
        String protocolVersion = headers.get(io.modelcontextprotocol.spec.HttpHeaders.PROTOCOL_VERSION);
        if (protocolVersion != null && !protocolVersions.contains(protocolVersion)) {
            return error(HttpStatus.BAD_REQUEST, "Unsupported protocol version: " + protocolVersion + ", supported versions: " + protocolVersions);
        }
        return null;
    }

    private boolean isAllowed(String origin) {
        if (anyOrigin || allowedOrigins.contains(normalize(origin))) {
            return true;
        }
        try {
            String host = URI.create(origin).getHost();
            // An attacker controlled name resolving to a loopback address still sends its own name as the origin
            return host != null && LOOPBACK_HOSTS.contains(host.toLowerCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static String normalize(String origin) {
        String normalized = origin.trim().toLowerCase(Locale.ROOT);
        return normalized.endsWith("/") ? normalized.substring(0, normalized.length() - 1) : normalized;
    }

    private static HttpResponse<?> error(HttpStatus status, String message) {
        // Not a response to a request the server read, so the id is null, which the SDK response type does not allow
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("jsonrpc", McpSchema.JSONRPC_VERSION);
        body.put("id", null);
        body.put("error", Map.of("code", McpSchema.ErrorCodes.INVALID_REQUEST, "message", message));
        return HttpResponse.status(status).body(Collections.unmodifiableMap(body));
    }
}
