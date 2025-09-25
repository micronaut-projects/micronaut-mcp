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
package io.micronaut.mcp.server.context;

import io.micronaut.core.annotation.Nullable;
import io.modelcontextprotocol.common.McpTransportContext;

import java.security.Principal;
import java.util.Locale;

/**
 * Extension of {@link McpTransportContext} with convenience methods to access common transport metadata in a Micronaut context.
 */
public interface MicronautMcpTransportContext extends McpTransportContext {
    /**
     *
     * @return The Locale of the request, if available
     */
    @Nullable
    Locale locale();

    /**
     *
     * @return The server host if available
     */
    @Nullable
    String host();


    /**
     *
     * @return The authenticated principal if available
     */
    @Nullable
    Principal principal();

    /**
     *
     * @return the last event ID if available
     */
    @Nullable
    String lastEventId();

    /**
     *
     * @return the session ID if available
     */
    @Nullable
    String sessionId();

    /**
     *
     * @return the MCP Protocol version
     */
    @Nullable
    String protocolVersion();
}
