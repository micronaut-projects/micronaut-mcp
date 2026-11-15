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

import io.micronaut.core.annotation.Internal;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.Duration;

/**
 * The properties common to the connections of every transport.
 */
@Internal
abstract class AbstractMcpClientConnectionConfigurationProperties implements McpClientConnectionConfiguration {
    private final String name;
    private @Nullable Duration initializationTimeout;
    private @Nullable Duration requestTimeout;
    private boolean autoHealthCheck = true;

    /**
     * @param name The name of the connection
     */
    AbstractMcpClientConnectionConfigurationProperties(String name) {
        this.name = name;
    }

    @Override
    public @NonNull String getName() {
        return name;
    }

    @Override
    public @Nullable Duration getInitializationTimeout() {
        return initializationTimeout;
    }

    /**
     * @param initializationTimeout How long to wait for the server to answer the initialization
     */
    public void setInitializationTimeout(@Nullable Duration initializationTimeout) {
        this.initializationTimeout = initializationTimeout;
    }

    @Override
    public @Nullable Duration getRequestTimeout() {
        return requestTimeout;
    }

    /**
     * @param requestTimeout How long to wait for the server to answer a request, such as a tool call
     */
    public void setRequestTimeout(@Nullable Duration requestTimeout) {
        this.requestTimeout = requestTimeout;
    }

    @Override
    public boolean isAutoHealthCheck() {
        return autoHealthCheck;
    }

    /**
     * Whether the LangChain4j client checks periodically that the server is reachable, and reconnects when it is not.
     * Default value {@code true}.
     *
     * @param autoHealthCheck Whether to check the server periodically
     */
    public void setAutoHealthCheck(boolean autoHealthCheck) {
        this.autoHealthCheck = autoHealthCheck;
    }
}
