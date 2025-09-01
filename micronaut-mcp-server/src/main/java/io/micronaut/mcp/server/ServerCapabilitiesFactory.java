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
package io.micronaut.mcp.server;

import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Prototype;
import io.micronaut.core.annotation.Internal;
import io.modelcontextprotocol.spec.McpSchema;

import java.util.Map;

/**
 * Creates prototype instance of {@link McpSchema.ServerCapabilities.Builder} and {@link McpSchema.ServerCapabilities}.
 *
 * @since 1.0.0
 */
@Internal
@Factory
final class ServerCapabilitiesFactory {
    @Prototype
    McpSchema.ServerCapabilities.Builder createServerCapabilitiesBuilder() {
        return McpSchema.ServerCapabilities.builder();
    }

    @Prototype
    McpSchema.ServerCapabilities createServerCapabilities(McpSchema.ServerCapabilities.Builder builder) {
        return builder.build();
    }
}
