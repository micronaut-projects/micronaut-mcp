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

import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.exceptions.ConfigurationException;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.order.Ordered;
import io.micronaut.core.util.StringUtils;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.RequestFilter;
import io.micronaut.http.annotation.ServerFilter;
import io.micronaut.http.filter.ServerFilterPhase;
import io.micronaut.mcp.conf.server.TransportSecurityConfiguration;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpStatelessServerTransport;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * request to the blocking executor. It runs just before the security filter: after the CORS filter, so that a browser
 * can read the rejection and the preflight is answered by CORS alone, and after the metrics and tracing filters, so
 * that they record rejected requests, but before authentication, so that a request from a foreign origin is answered
 * with 403 rather than 401. It is created with the context, so that an allowed origin that could never match fails the
 * startup.
 *
 * @see <a href="https://modelcontextprotocol.io/specification/2025-11-25/basic/transports#security-warning">Streamable HTTP security</a>
 * @see <a href="https://modelcontextprotocol.io/specification/2025-11-25/basic/transports#protocol-version-header">Protocol version header</a>
 */
@ServerFilter(McpController.PATH)
@Context
@Requires(property = TransportSecurityConfiguration.PROPERTY_ENABLED, notEquals = StringUtils.FALSE)
@Internal
final class McpTransportSecurityFilter implements Ordered {
    private static final Logger LOG = LoggerFactory.getLogger(McpTransportSecurityFilter.class);
    private static final String WILDCARD = "*";
    private static final String ALLOWED_ORIGINS = TransportSecurityConfiguration.PREFIX + ".allowed-origins";
    private static final String SCHEME_SEPARATOR = "://";
    private static final Set<String> LOOPBACK_HOSTS = Set.of("localhost", "127.0.0.1", "[::1]");

    private final boolean anyOrigin;
    private final boolean allowLoopbackOrigins;
    private final Set<String> allowedOrigins;
    private final List<String> protocolVersions;

    McpTransportSecurityFilter(TransportSecurityConfiguration configuration, McpStatelessServerTransport transport) {
        this.allowLoopbackOrigins = configuration.isAllowLoopbackOrigins();
        this.allowedOrigins = configuration.getAllowedOrigins().stream()
            .map(McpTransportSecurityFilter::allowedOrigin)
            .collect(Collectors.toUnmodifiableSet());
        this.anyOrigin = allowedOrigins.contains(WILDCARD);
        // The versions the SDK transport supports: an SDK upgrade that drops a version starts rejecting clients that
        // send it, which TransportSecurityTest pins
        this.protocolVersions = transport.protocolVersions();
    }

    @Override
    public int getOrder() {
        return ServerFilterPhase.SECURITY.before();
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
            if (LOG.isDebugEnabled()) {
                LOG.debug("Rejected request to {}: Origin '{}' is not listed in {}{}", request.getPath(), sanitize(origin),
                    ALLOWED_ORIGINS, allowLoopbackOrigins ? " and is not a loopback origin" : "");
            }
            return error(HttpStatus.FORBIDDEN, "Origin not allowed: " + origin);
        }
        String protocolVersion = headers.get(io.modelcontextprotocol.spec.HttpHeaders.PROTOCOL_VERSION);
        if (protocolVersion != null && !protocolVersions.contains(protocolVersion)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Rejected request to {}: MCP-Protocol-Version '{}' is not supported (supported: {})",
                    request.getPath(), sanitize(protocolVersion), protocolVersions);
            }
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
        if (!isPortSuffix(origin, end) || origin.lastIndexOf('@', end) >= start) {
            // A path, or user information, which an origin never has
            return "";
        }
        return origin.substring(start, end).toLowerCase(Locale.ROOT);
    }

    /**
     * Whether the origin ends at the given index, or continues with nothing but a port ({@code :digits}) and an
     * optional trailing slash.
     */
    private static boolean isPortSuffix(String origin, int index) {
        int end = origin.endsWith("/") ? origin.length() - 1 : origin.length();
        if (index == end) {
            return true;
        }
        if (origin.charAt(index) != ':' || index + 1 == end) {
            return false;
        }
        for (int i = index + 1; i < end; i++) {
            char c = origin.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }

    /**
     * A configured origin, normalized.
     *
     * @throws ConfigurationException if it is neither {@code *} nor an origin, so that it could never match
     */
    private static String allowedOrigin(String entry) {
        String origin = normalize(entry);
        if (origin.equals(WILDCARD) || isOrigin(origin)) {
            return origin;
        }
        String prefix = "Invalid " + ALLOWED_ORIGINS + " entry '" + entry + "': ";
        if (origin.equals("null")) {
            throw new ConfigurationException(prefix + "clients that send Origin: null, such as file:// pages and sandboxed "
                + "iframes, can only be allowed with *");
        }
        if (origin.indexOf('*') >= 0) {
            throw new ConfigurationException(prefix + "wildcards are not supported in an origin; list each origin, for "
                + "example https://app.example.com, or use * to allow every origin");
        }
        String example = example(origin);
        throw new ConfigurationException(prefix + "expected * or an origin, scheme://host[:port], without a path"
            + (example != null ? ", for example '" + example + "'" : ""));
    }

    private static boolean isOrigin(String origin) {
        return origin.indexOf(SCHEME_SEPARATOR) > 0 && isHost(host(origin));
    }

    /**
     * Whether the host, as {@link #host(String)} returns it, is a host name, an IPv4 address or a bracketed IPv6 address,
     * in the ASCII form browsers send.
     */
    private static boolean isHost(String host) {
        boolean ipv6 = host.startsWith("[");
        if (host.length() < (ipv6 ? 3 : 1)) {
            return false;
        }
        for (int i = ipv6 ? 1 : 0; i < host.length() - (ipv6 ? 1 : 0); i++) {
            char c = host.charAt(i);
            boolean valid = ipv6
                ? (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || c == ':' || c == '.'
                : (c >= '0' && c <= '9') || (c >= 'a' && c <= 'z') || c == '-' || c == '.' || c == '_';
            if (!valid) {
                return false;
            }
        }
        return true;
    }

    /**
     * The origin that an invalid entry most likely meant: with {@code https://} added when it has no scheme, and
     * without its path.
     */
    private static @Nullable String example(String entry) {
        String candidate = entry.contains(SCHEME_SEPARATOR) ? entry : "https://" + entry;
        int path = candidate.indexOf('/', candidate.indexOf(SCHEME_SEPARATOR) + SCHEME_SEPARATOR.length());
        if (path >= 0) {
            candidate = candidate.substring(0, path);
        }
        return isOrigin(candidate) ? candidate : null;
    }

    /**
     * A request header value without control characters, so that it cannot forge log lines.
     */
    private static String sanitize(String value) {
        StringBuilder sanitized = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            sanitized.append(c < ' ' || c == 0x7f ? '?' : c);
        }
        return sanitized.toString();
    }

    /**
     * An origin in lower case, without a trailing slash and without the default port of {@code http} and {@code https},
     * which browsers leave out.
     */
    private static String normalize(String origin) {
        String normalized = origin.trim().toLowerCase(Locale.ROOT);
        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.startsWith("http://") && normalized.endsWith(":80")) {
            return normalized.substring(0, normalized.length() - ":80".length());
        }
        if (normalized.startsWith("https://") && normalized.endsWith(":443")) {
            return normalized.substring(0, normalized.length() - ":443".length());
        }
        return normalized;
    }

    private static HttpResponse<?> error(HttpStatus status, String message) {
        // Not a response to a request the server read, so the id is null, which the SDK response type does not allow
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("jsonrpc", McpSchema.JSONRPC_VERSION);
        body.put("id", null);
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("code", McpSchema.ErrorCodes.INVALID_REQUEST);
        error.put("message", message);
        body.put("error", Collections.unmodifiableMap(error));
        return HttpResponse.status(status).body(Collections.unmodifiableMap(body));
    }
}
