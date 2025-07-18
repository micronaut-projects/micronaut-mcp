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

import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.util.Toggleable;

/**
 * MCP Controller configuration.
 */
public interface McpControllerConfiguration extends Toggleable {
    String PREFIX = "micronaut.mcp.endpoint";
    String DEFAULT_PATH = "/mcp";
    boolean DEFAULT_ENABLED = true;
    String PROPERTY_ENABLED = PREFIX + ".enabled";
    String PROPERTY_PATH = PREFIX + ".path";

    /**
     * @return the path where the controller is enabled.
     */
    @NonNull
    default String getPath() {
        return DEFAULT_PATH;
    }

    @Override
    default boolean isEnabled() {
        return DEFAULT_ENABLED;
    }
}
