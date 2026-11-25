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
package io.micronaut.mcp.server.stdio;

import java.util.List;
import java.util.Objects;

import io.micronaut.core.annotation.Internal;
import io.micronaut.mcp.server.exceptions.JsonRrpcResponseUtils;
import io.modelcontextprotocol.json.TypeRef;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpServerSession;
import io.modelcontextprotocol.spec.McpServerTransport;
import io.modelcontextprotocol.spec.McpServerTransportProvider;
import reactor.core.publisher.Mono;

/**
 * Decorates a {@link McpServerTransportProvider} so that listing requests for primitives the server has no capability for
 * (for example {@code tools/list} on a server without tools) are answered with an empty list instead of a
 * {@code Method not found} error.
 *
 * <p>Everything is delegated to the decorated provider. The only change is that the {@link McpServerTransport} handed to the
 * session normalizes outbound {@link McpSchema.JSONRPCResponse} values via
 * {@link JsonRrpcResponseUtils#map(McpSchema.JSONRPCResponse)} before sending them.</p>
 */
@Internal
final class GracefulListingServerTransportProvider implements McpServerTransportProvider {

    private final McpServerTransportProvider delegate;

    /**
     * @param delegate The transport provider to decorate
     */
    GracefulListingServerTransportProvider(McpServerTransportProvider delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    @Override
    public void setSessionFactory(McpServerSession.Factory sessionFactory) {
        delegate.setSessionFactory(transport -> sessionFactory.create(new GracefulListingServerTransport(transport)));
    }

    @Override
    public Mono<Void> notifyClients(String method, Object params) {
        return delegate.notifyClients(method, params);
    }

    @Override
    public Mono<Void> notifyClient(String sessionId, String method, Object params) {
        return delegate.notifyClient(sessionId, method, params);
    }

    @Override
    public Mono<Void> closeGracefully() {
        return delegate.closeGracefully();
    }

    @Override
    public void close() {
        delegate.close();
    }

    @Override
    public List<String> protocolVersions() {
        return delegate.protocolVersions();
    }

    private static final class GracefulListingServerTransport implements McpServerTransport {

        private final McpServerTransport delegate;

        private GracefulListingServerTransport(McpServerTransport delegate) {
            this.delegate = delegate;
        }

        @Override
        public Mono<Void> sendMessage(McpSchema.JSONRPCMessage message) {
            if (message instanceof McpSchema.JSONRPCResponse jsonrpcResponse) {
                return delegate.sendMessage(JsonRrpcResponseUtils.map(jsonrpcResponse));
            }
            return delegate.sendMessage(message);
        }

        @Override
        public <T> T unmarshalFrom(Object data, TypeRef<T> typeRef) {
            return delegate.unmarshalFrom(data, typeRef);
        }

        @Override
        public Mono<Void> closeGracefully() {
            return delegate.closeGracefully();
        }

        @Override
        public void close() {
            delegate.close();
        }

        @Override
        public List<String> protocolVersions() {
            return delegate.protocolVersions();
        }
    }
}
