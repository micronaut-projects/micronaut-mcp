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

import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.annotation.RequestBean;

/**
 * @see <a href="https://modelcontextprotocol.io/specification/2025-06-18/basic/transports#protocol-version-header">Protocol Version Header</a>
 * @param protocolVersion MCP Version
 * @param sessionId Session ID
 * @param lastEventId Last Event ID
 */
@Introspected
public record McpHttpRequest(@Header(name = HTTP_HEADER_MCP_PROTOCOL_VERSION, defaultValue = DEFAULT_PROTOCOL_VERSION) @NonNull String protocolVersion,
                             @Header(HTTP_HEADER_MCP_SESSION_ID) @Nullable String sessionId,
                             @Header(HTTP_HEADER_DEFAULT_LAST_EVENT_ID) @Nullable String lastEventId) {
    public static final String HTTP_HEADER_MCP_PROTOCOL_VERSION = "MCP-Protocol-Version";
    public static final String DEFAULT_PROTOCOL_VERSION = "2025-03-26";
    public static final String HTTP_HEADER_MCP_SESSION_ID = "Mcp-Session-Id";
    public static final String HTTP_HEADER_DEFAULT_LAST_EVENT_ID = "Last-Event-ID";
}
