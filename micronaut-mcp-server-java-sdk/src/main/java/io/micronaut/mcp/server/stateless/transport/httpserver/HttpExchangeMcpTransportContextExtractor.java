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
package io.micronaut.mcp.server.stateless.transport.httpserver;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.server.McpTransportContextExtractor;
import io.modelcontextprotocol.spec.ProtocolVersions;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * {@link McpTransportContextExtractor} implementation for {@link HttpExchange}.
 */
public class HttpExchangeMcpTransportContextExtractor implements McpTransportContextExtractor<HttpExchange> {

    @Override
    public McpTransportContext extract(HttpExchange httpExchange) {
        return McpTransportContext.create(metadata(httpExchange));
    }

    private Map<String, Object> metadata(HttpExchange httpExchange) {
        Headers headers = httpExchange.getRequestHeaders();
        return metadata(headers);
    }

    private Map<String, Object> metadata(Headers headers) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put(io.modelcontextprotocol.spec.HttpHeaders.PROTOCOL_VERSION,
            Optional.ofNullable(headers.getFirst(io.modelcontextprotocol.spec.HttpHeaders.PROTOCOL_VERSION))
                .orElse(ProtocolVersions.MCP_2025_03_26));
        Optional.ofNullable(headers.getFirst(io.modelcontextprotocol.spec.HttpHeaders.MCP_SESSION_ID))
            .ifPresent(v -> metadata.put(io.modelcontextprotocol.spec.HttpHeaders.MCP_SESSION_ID, v));
        Optional.ofNullable(headers.getFirst(io.modelcontextprotocol.spec.HttpHeaders.LAST_EVENT_ID))
            .ifPresent(v -> metadata.put(io.modelcontextprotocol.spec.HttpHeaders.LAST_EVENT_ID, v));
        return metadata;
    }
}
