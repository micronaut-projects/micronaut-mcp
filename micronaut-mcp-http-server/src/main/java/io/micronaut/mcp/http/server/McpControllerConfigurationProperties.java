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

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.core.annotation.Internal;

/**
 * {@link McpControllerConfiguration} for Diff Controller.
 */
@ConfigurationProperties(McpControllerConfiguration.PREFIX)
@Internal
class McpControllerConfigurationProperties implements McpControllerConfiguration {
    private boolean enabled = DEFAULT_ENABLED;
    private String path = DEFAULT_PATH;

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    /**
     *
     * @param enabled Whether the controller is enabled. Default value: {@value #DEFAULT_ENABLED}.
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public String getPath() {
        return path;
    }

    /**
     *
     * @param path Controller path. Default value: /api/v1/diff
     */
    public void setPath(String path) {
        this.path = path;
    }
}
