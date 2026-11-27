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
package io.micronaut.mcp.client.http;

import io.micronaut.core.annotation.Internal;

/**
 * Raised when the MCP server no longer knows the session of the client, which must initialize a new one.
 */
@Internal
public final class McpHttpSessionNotFoundException extends RuntimeException {
    private final String sessionId;

    /**
     * @param sessionId The id of the session
     * @param message The message
     * @param cause The response of the server
     */
    public McpHttpSessionNotFoundException(String sessionId, String message, Throwable cause) {
        super(message, cause);
        this.sessionId = sessionId;
    }

    /**
     * @return The id of the session
     */
    public String getSessionId() {
        return sessionId;
    }
}
