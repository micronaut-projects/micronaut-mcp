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
package io.micronaut.mcp.server.stdio;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.naming.Described;
import io.micronaut.mcp.conf.McpServerConfiguration;
import io.micronaut.runtime.ApplicationConfiguration;
import io.micronaut.runtime.EmbeddedApplication;
import io.micronaut.runtime.event.ApplicationShutdownEvent;
import io.micronaut.runtime.event.ApplicationStartupEvent;
import io.micronaut.runtime.exceptions.ApplicationStartupException;
import jakarta.inject.Singleton;

/**
 * An alternative {@link EmbeddedApplication} that gets activated for MCP Server using stdio transport when no other application is present.
 */
@Singleton
@Requires(missingBeans = EmbeddedApplication.class)
@Internal
class McpStdioApplication implements EmbeddedApplication<McpStdioApplication>, Described {

    private final ApplicationContext applicationContext;
    private final ApplicationConfiguration configuration;
    private final McpServerConfiguration mcpServerConfiguration;

    /**
     * Constructs a new messaging application.
     *
     * @param applicationContext The context
     * @param configuration The configuration
     * @param mcpServerConfiguration MCP Server configuration
     */
    McpStdioApplication(ApplicationContext applicationContext,
                        ApplicationConfiguration configuration,
                        McpServerConfiguration mcpServerConfiguration) {
        this.applicationContext = applicationContext;
        this.configuration = configuration;
        this.mcpServerConfiguration = mcpServerConfiguration;
    }

    @Override
    public @NonNull ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    @Override
    public ApplicationConfiguration getApplicationConfiguration() {
        return configuration;
    }

    @Override
    public boolean isRunning() {
        return applicationContext.isRunning();
    }

    @Override
    public boolean isServer() {
        return true;
    }

    @Override
    @NonNull
    public McpStdioApplication start() {
        ApplicationContext applicationContext = getApplicationContext();
        if (!applicationContext.isRunning()) {
            try {
                applicationContext.start();
            } catch (Throwable e) {
                throw new ApplicationStartupException("Error starting MCP server: " + e.getMessage(), e);
            }
        }
        applicationContext.publishEvent(new ApplicationStartupEvent(this));
        return this;
    }

    @Override
    @NonNull
    public McpStdioApplication stop() {
        ApplicationContext applicationContext = getApplicationContext();
        if (applicationContext.isRunning()) {
            applicationContext.publishEvent(new ApplicationShutdownEvent(this));
            applicationContext.stop();
        }
        return this;
    }

    @Override
    public @NonNull String getDescription() {
        return mcpServerConfiguration.getTransport() + " " + (mcpServerConfiguration.isReactive() ? "async" : "sync");
    }
}

