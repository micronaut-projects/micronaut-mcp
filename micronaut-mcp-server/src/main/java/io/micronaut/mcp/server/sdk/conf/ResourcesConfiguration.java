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
package io.micronaut.mcp.server.sdk.conf;

/**
 * MCP Resources configuration.
 * @since 1.0.0
 */
public interface ResourcesConfiguration extends McpPrimitiveConfiguration {
    String PREFIX = McpServerConfiguration.PREFIX + ".resources";
    boolean DEFAULT_SUBSCRIBE = false;

    /**
     *
     * @return whether the client can subscribe to be notified of changes to individual resources.
     */
    default boolean isSubscribe() {
        return DEFAULT_SUBSCRIBE;
    }

    /**
     *
     * @return whether the server will emit notifications when the list of available resources changes.
     */
    @Override
    default boolean isListChanged() {
        return DEFAULT_LIST_CHANGED;
    }
}
