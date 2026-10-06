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
package io.micronaut.mcp.conf.server;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * MCP Server Info Configuration.
 * @since 1.0.0
 */
public interface McpServerInfoConfiguration {
    /**
     * MCP Server Info configuration Prefix.
     */
    String PREFIX = McpServerConfiguration.PREFIX + ".info";

    /**
     * configuration property name for MCP Server name.
     */
    String PROPERTY_NAME = PREFIX + ".name";

    /**
     * configuration property name for an MCP Server version.
     */
    String PROPERTY_VERSION = PREFIX + ".version";

    /**
     *
     * @return Server Name
     */
    @NonNull
    String getName();

    /**
     *
     * @return Server Version
     */
    @NonNull
    String getVersion();

    /**
     * @return A human-readable title of the server, for display
     * @since 2.2.0
     */
    default @Nullable String getTitle() {
        return null;
    }

    /**
     * @return A description of the server
     * @since 2.2.0
     */
    default @Nullable String getDescription() {
        return null;
    }

    /**
     * @return The URL of the website of the server
     * @since 2.2.0
     */
    default @Nullable String getWebsiteUrl() {
        return null;
    }

    /**
     * @return The icons of the server
     * @since 2.2.0
     */
    default @NonNull List<IconConfiguration> getIcons() {
        return List.of();
    }

    /**
     * @return Instructions describing how to use the server and its features, which clients may add to the model's prompt
     * @since 2.2.0
     */
    default @Nullable String getInstructions() {
        return null;
    }
}
