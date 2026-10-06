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

import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.util.StringUtils;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.RequestFilter;
import io.micronaut.http.annotation.ServerFilter;
import io.micronaut.mcp.conf.server.TransportSecurityConfiguration;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpStatelessServerTransport;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validates the {@code Origin} and {@code MCP-Protocol-Version} headers of requests to the MCP endpoint. As a filter it
 * runs on the thread that received the request, before the body is read and before a blocking server hands the
 * request to the blocking executor.
 *
 * @see <a href="https://modelcontextprotocol.io/specification/2025-11-25/basic/transports#security-warning">Streamable HTTP security</a>
 * @see <a href="https://modelcontextprotocol.io/specification/2025-11-25/basic/transports#protocol-version-header">Protocol version header</a>
 */
@ServerFilter(McpController.PATH)
@Requires(property = TransportSecurityConfiguration.PREFIX + ".enabled", notEquals = StringUtils.FALSE)
@Internal
final class McpTransportSecurityFilter {
    private static final String WILDCARD = "*";
    private static final String SCHEME_SEPARATOR = "://";
    private static final Set<String> LOOPBACK_HOSTS = Set.of("localhost", "127.0.0.1", "[::1]");

    private final boolean anyOrigin;
    private final boolean allowLoopbackOrigins;
    private final Set<String> allowedOrigins;
    private final List<String> protocolVersions;

    McpTransportSecurityFilter(TransportSecurityConfiguration configuration, McpStatelessServerTransport transport) {
        this.anyOrigin = configuration.getAllowedOrigins().contains(WILDCARD);
        this.allowLoopbackOrigins = configuration.isAllowLoopbackOrigins();
        this.allowedOrigins = configuration.getAllowedOrigins().stream()
            .map(McpTransportSecurityFilter::normalize)
            .collect(Collectors.toUnmodifiableSet());
        this.protocolVersions = transport.protocolVersions();
    }

    /**
     * @param request The request
     * @return The response rejecting the request, or {@code null} when it is valid
     */
    // The rejection body is a JSON-RPC error map, so the response body type is a wildcard
    @SuppressWarnings("java:S1452")
    @RequestFilter
    @Nullable HttpResponse<?> filter(HttpRequest<?> request) {
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
        // An attacker controlled name resolving to a loopback address still sends its own name as the origin
        return allowLoopbackOrigins && LOOPBACK_HOSTS.contains(host(origin));
    }

    /**
     * The host of an origin ({@code scheme://host[:port]}), read without parsing it as a URI, in lower case, or an empty
     * string when the origin has no host.
     */
    private static String host(String origin) {
        int start = origin.indexOf(SCHEME_SEPARATOR);
        if (start < 0) {
            return "";
        }
        start += SCHEME_SEPARATOR.length();
        int end;
        if (origin.startsWith("[", start)) {
            // An IPv6 literal, kept with its brackets
            end = origin.indexOf(']', start);
            if (end < 0) {
                return "";
            }
            end++;
        } else {
            end = start;
            while (end < origin.length() && origin.charAt(end) != ':' && origin.charAt(end) != '/') {
                end++;
            }
        }
        if (end < origin.length() && origin.charAt(end) != ':' && origin.charAt(end) != '/') {
            return "";
        }
        return origin.substring(start, end).toLowerCase(Locale.ROOT);
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
