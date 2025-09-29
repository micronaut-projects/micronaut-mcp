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
package io.micronaut.mcp.client.javasdk;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.naming.Named;

import java.net.URI;
import java.time.Duration;

/**
 * MCP Client HTTP Configuration.
 */
public interface McpClientHtttpConfiguration extends Named {
    String PREFIX = "micronaut.mcp.client.http";

    /**
     *
     * @return The URL of the MCP Server
     */
    @NonNull
    URI getUrl();

    /**
     *
     * @return Sets the duration to wait for server responses before timing out requests.
     */
    @Nullable
    Duration getRequestTimeout();
}
