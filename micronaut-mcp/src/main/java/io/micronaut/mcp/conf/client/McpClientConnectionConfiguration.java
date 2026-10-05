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
package io.micronaut.mcp.conf.client;

import io.micronaut.core.naming.Named;
import org.jspecify.annotations.Nullable;

import java.time.Duration;

/**
 * The configuration of a named connection to an MCP server, over any transport. A client is created for each connection.
 *
 * @since 2.2.0
 */
public interface McpClientConnectionConfiguration extends Named {

    /**
     * @return How long to wait for the server to answer the initialization, or {@code null} for the client default
     */
    default @Nullable Duration getInitializationTimeout() {
        return null;
    }

    /**
     * @return How long to wait for the server to answer a request, such as a tool call, or {@code null} for the client default
     */
    default @Nullable Duration getRequestTimeout() {
        return null;
    }
}
