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
import io.micronaut.mcp.client.http.MicronautMcpHttpExchange;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.TypeRef;
import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.ProtocolVersions;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * An MCP Java SDK client transport over Streamable HTTP, on the Micronaut HTTP client.
 */
@Internal
final class MicronautStreamableHttpClientTransport implements McpClientTransport {
    private static final List<String> PROTOCOL_VERSIONS = List.of(ProtocolVersions.MCP_2025_03_26,
        ProtocolVersions.MCP_2025_06_18, ProtocolVersions.MCP_2025_11_25);

    private final MicronautMcpHttpExchange exchange;
    private final McpJsonMapper jsonMapper;
    private volatile Function<Mono<McpSchema.JSONRPCMessage>, Mono<McpSchema.JSONRPCMessage>> handler = Function.identity();

    MicronautStreamableHttpClientTransport(MicronautMcpHttpExchange exchange, McpJsonMapper jsonMapper) {
        this.exchange = exchange;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public Mono<Void> connect(Function<Mono<McpSchema.JSONRPCMessage>, Mono<McpSchema.JSONRPCMessage>> handler) {
        this.handler = handler;
        return Mono.empty();
    }

    @Override
    public Mono<Void> sendMessage(McpSchema.JSONRPCMessage message) {
        String json;
        try {
            json = jsonMapper.writeValueAsString(message);
        } catch (IOException e) {
            return Mono.error(e);
        }
        return exchange.post(json)
            .concatMap(this::receive)
            .then();
    }

    private Mono<Void> receive(String json) {
        McpSchema.JSONRPCMessage message;
        try {
            message = McpSchema.deserializeJsonRpcMessage(jsonMapper, json);
        } catch (IOException e) {
            return Mono.error(e);
        }
        if (message instanceof McpSchema.JSONRPCResponse response
            && response.result() instanceof Map<?, ?> result
            && result.get("protocolVersion") instanceof String version
            && result.containsKey("serverInfo")) {
            // The response to initialize: later requests carry the negotiated version
            exchange.setProtocolVersion(version);
        }
        // The client session answers requests of the server, such as sampling, by sending a message itself
        return handler.apply(Mono.just(message)).then();
    }

    @Override
    public Mono<Void> closeGracefully() {
        return exchange.close();
    }

    @Override
    public <T> T unmarshalFrom(Object data, TypeRef<T> typeRef) {
        return jsonMapper.convertValue(data, typeRef);
    }

    @Override
    public List<String> protocolVersions() {
        return PROTOCOL_VERSIONS;
    }
}
