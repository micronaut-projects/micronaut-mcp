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
package io.micronaut.mcp.http.server;

import io.micronaut.core.convert.ArgumentConversionContext;
import io.micronaut.core.type.Argument;
import io.micronaut.core.util.StringUtils;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.bind.binders.TypedRequestArgumentBinder;
import jakarta.inject.Singleton;

import java.util.Optional;

import static io.micronaut.mcp.http.server.McpHttpRequest.*;

/**
 * {@link TypedRequestArgumentBinder} to bind MCP HTTP Headers into a {@link McpHttpRequest} object.
 */
@Singleton
public class McpRequestTypedRequestArgumentBinder implements TypedRequestArgumentBinder<McpHttpRequest> {
    private static final Argument<McpHttpRequest> ARG = Argument.of(McpHttpRequest.class);

    @Override
    public Argument<McpHttpRequest> argumentType() {
        return ARG;
    }

    @Override
    public BindingResult<McpHttpRequest> bind(ArgumentConversionContext<McpHttpRequest> context, HttpRequest<?> source) {
        final String mcpProtocolVersionHeader = source.getHeaders().get(HTTP_HEADER_MCP_PROTOCOL_VERSION);
        final String mcpSessionIdHeader = source.getHeaders().get(HTTP_HEADER_MCP_SESSION_ID);
        final String lastEventId = source.getHeaders().get(HTTP_HEADER_DEFAULT_LAST_EVENT_ID);
        // If the header is not present use 2025-03-26
        // https://modelcontextprotocol.io/specification/2025-06-18/basic/transports#protocol-version-header
        // > For backwards compatibility, if the server does not receive an MCP-Protocol-Version header,
        // > and has no other way to identify the version - for example, by relying on the protocol
        // version negotiated during initialization -
        // the server SHOULD assume protocol version 2025-03-26.
        String mcpProtocolVersion = StringUtils.isNotEmpty(mcpProtocolVersionHeader) ? mcpProtocolVersionHeader : DEFAULT_PROTOCOL_VERSION;

        return () -> Optional.of(new McpHttpRequest(mcpProtocolVersion, mcpSessionIdHeader, lastEventId));
    }
}
