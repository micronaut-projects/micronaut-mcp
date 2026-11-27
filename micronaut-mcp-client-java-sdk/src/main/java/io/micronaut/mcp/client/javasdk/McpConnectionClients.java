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
package io.micronaut.mcp.client.javasdk;

import io.micronaut.core.annotation.Internal;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The state shared by the clients of each connection: which kind of client, synchronous or asynchronous, the connection
 * has, since each client opens its own session over its own transport, and the cached tools of its server.
 */
@Singleton
@Internal
final class McpConnectionClients {
    private final Map<String, Claim> claims = new ConcurrentHashMap<>();
    private final Map<String, List<McpSchema.Tool>> tools = new ConcurrentHashMap<>();

    /**
     * Records that a client of the given kind is created for a connection.
     *
     * @param connection The name of the connection
     * @param kind The kind of client
     * @return The claim for this particular client creation
     * @throws IllegalStateException If the connection has a client of the other kind
     */
    Claim claim(String connection, Class<?> kind) {
        return claims.compute(connection, (name, existing) -> {
            if (existing != null && existing.kind != kind) {
                throw new IllegalStateException("The MCP connection " + name + " already has a client of type "
                    + existing.kind.getSimpleName() + ". Use either the McpSyncClient or the McpAsyncClient of a connection, not both: "
                    + "each would open its own session, and start its own process over STDIO. McpClientTools uses the McpSyncClient.");
            }
            return new Claim(kind);
        });
    }

    /**
     * Forgets the client of a connection, which failed to initialize or is closed.
     *
     * @param connection The name of the connection
     * @param claim The claim acquired when this client was created
     */
    void release(String connection, Claim claim) {
        claims.computeIfPresent(connection, (name, current) -> {
            if (current == claim) {
                tools.remove(name);
                return null;
            }
            return current;
        });
    }

    /**
     * @param connection The name of the connection
     * @return The cached tools of the server, or {@code null} if they were not listed yet
     */
    @Nullable List<McpSchema.Tool> tools(String connection) {
        return tools.get(connection);
    }

    /**
     * Caches the tools of a server, which notified that they changed.
     *
     * @param connection The name of the connection
     * @param serverTools The tools of the server
     */
    void toolsChanged(String connection, List<McpSchema.Tool> serverTools) {
        tools.put(connection, List.copyOf(serverTools));
    }

    /**
     * Caches the tools of a server that were listed, unless they changed meanwhile.
     *
     * @param connection The name of the connection
     * @param serverTools The tools of the server
     * @return The cached tools
     */
    List<McpSchema.Tool> toolsListed(String connection, List<McpSchema.Tool> serverTools) {
        List<McpSchema.Tool> listed = List.copyOf(serverTools);
        List<McpSchema.Tool> existing = tools.putIfAbsent(connection, listed);
        return existing != null ? existing : listed;
    }

    /**
     * Identifies one client creation while destruction and replacement of a singleton overlap.
     */
    static final class Claim {
        private final Class<?> kind;

        private Claim(Class<?> kind) {
            this.kind = kind;
        }
    }
}
