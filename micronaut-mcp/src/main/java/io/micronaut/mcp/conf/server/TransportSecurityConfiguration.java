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
package io.micronaut.mcp.conf.server;

import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * Protection of the Streamable HTTP endpoint against DNS rebinding and unsupported clients.
 *
 * @see <a href="https://modelcontextprotocol.io/specification/2025-11-25/basic/transports#security-warning">Streamable HTTP security</a>
 * @since 2.2.0
 */
public interface TransportSecurityConfiguration {
    /**
     * Transport security configuration prefix.
     */
    String PREFIX = McpServerConfiguration.PREFIX + ".transport-security";

    /**
     * Whether the {@code Origin} and {@code MCP-Protocol-Version} request headers are validated by default.
     */
    boolean DEFAULT_ENABLED = true;

    /**
     * Whether loopback origins are allowed by default.
     */
    boolean DEFAULT_ALLOW_LOOPBACK_ORIGINS = true;

    /**
     * @return Whether the {@code Origin} and {@code MCP-Protocol-Version} request headers are validated. A request with
     * an {@code Origin} that is not allowed is answered with 403, and one with an unsupported protocol version with 400.
     */
    boolean isEnabled();

    /**
     * @return The origins allowed besides loopback origins, such as {@code https://app.example.com}. {@code *} allows
     * every origin. Requests without an {@code Origin} header, which browsers always send, are allowed.
     */
    @NonNull
    List<String> getAllowedOrigins();

    /**
     * Whether loopback origins ({@code localhost}, {@code 127.0.0.1} and {@code [::1]}, with any scheme and port) are
     * allowed, so that local tools such as the MCP Inspector can connect. A server reachable from other hosts can
     * disable it, so that pages served by other local applications cannot call it.
     *
     * @return Whether loopback origins are allowed
     */
    default boolean isAllowLoopbackOrigins() {
        return DEFAULT_ALLOW_LOOPBACK_ORIGINS;
    }
}
