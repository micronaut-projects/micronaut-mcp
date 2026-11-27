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

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.core.annotation.Internal;

@ConfigurationProperties(ToolsConfiguration.PREFIX)
@Internal
final class ToolsConfigurationProperties implements ToolsConfiguration {
    private boolean listChanged;

    @Override
    public boolean isListChanged() {
        return listChanged;
    }

    /**
     *
     * @param listChanged whether the tools capability is advertised even when the server has no tools, so tools can be added at
     *                    runtime, and, over STDIO, whether the server notifies clients when the list of tools changes. The HTTP
     *                    transport always advertises `listChanged` as `false`. Default value `false`.
     */
    public void setListChanged(boolean listChanged) {
        this.listChanged = listChanged;
    }
}
