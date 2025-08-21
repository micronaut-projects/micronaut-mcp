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
import jakarta.inject.Singleton;

import java.util.Map;

/**
 * Creates prototype instance of {@link McpSchema.ServerCapabilities.Builder} and {@link McpSchema.ServerCapabilities}.
 *
 * @since 1.0.0
 */
@Internal
@Factory
final class ServerCapabilitiesFactory {
    @Singleton
    McpSchema.ServerCapabilities.Builder createServerCapabilitiesBuilder() {
        McpSchema.ServerCapabilities.Builder builder = McpSchema.ServerCapabilities.builder();
        //TODO builder.logging();
        return builder;
    }

    @Prototype
    McpSchema.ServerCapabilities createServerCapabilities(McpSchema.ServerCapabilities.Builder builder) {
        McpSchema.ServerCapabilities serverCapabilities = builder.build();

        // Workaround for https://github.com/modelcontextprotocol/java-sdk/pull/480
        McpSchema.ServerCapabilities.CompletionCapabilities completions = serverCapabilities.completions();
        Map<String, Object> experimental = serverCapabilities.experimental();
        McpSchema.ServerCapabilities.LoggingCapabilities logging = null;
        McpSchema.ServerCapabilities.PromptCapabilities prompts = serverCapabilities.prompts();
        McpSchema.ServerCapabilities.ResourceCapabilities resources = serverCapabilities.resources();
        McpSchema.ServerCapabilities.ToolCapabilities tools = serverCapabilities.tools();

        return new McpSchema.ServerCapabilities(completions, experimental, logging, prompts, resources, tools);
    }

}
