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
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.mcp.conf.server.PromptsConfiguration;
import io.micronaut.mcp.conf.server.ResourcesConfiguration;
import io.micronaut.mcp.conf.server.ToolsConfiguration;
import io.micronaut.mcp.server.registry.PromptRegistry;
import io.micronaut.mcp.server.registry.ToolRegistry;
import io.micronaut.mcp.server.registry.ResourceRegistry;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpStatelessServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;

import java.util.List;

/**
 * Creates prototype instance of {@link McpSchema.ServerCapabilities.Builder} and {@link McpSchema.ServerCapabilities}.
 * @since 1.0.0
 */
@Internal
@Factory
class ServerCapabilitiesFactory {
    @SuppressWarnings({"java:S107", "ParameterNumber"})
    @Prototype
    McpSchema.ServerCapabilities.Builder createServerCapabilitiesBuilder(
        List<McpServerFeatures.SyncCompletionSpecification> syncCompletions,
        List<McpServerFeatures.AsyncCompletionSpecification> asyncCompletions,
        List<McpStatelessServerFeatures.AsyncCompletionSpecification> statelessAsyncCompletions,
        List<McpStatelessServerFeatures.SyncCompletionSpecification> statelessSyncCompletions,
        ResourcesConfiguration resourcesConfiguration,
        List<McpSchema.ResourceTemplate> resourceTemplates,
        List<McpSchema.Resource> resources,
        List<McpServerFeatures.SyncResourceSpecification> syncResources,
        List<McpServerFeatures.AsyncResourceSpecification> asyncResources,
        List<McpStatelessServerFeatures.AsyncResourceSpecification> statelessAsyncResources,
        List<McpStatelessServerFeatures.SyncResourceSpecification> statelessSyncResources,
        ToolsConfiguration toolsConfiguration,
        ToolRegistry toolRegistry,
        ResourceRegistry resourceRegistry,
        List<McpServerFeatures.SyncToolSpecification> syncTools,
        List<McpServerFeatures.AsyncToolSpecification> asyncTools,
        List<McpStatelessServerFeatures.AsyncToolSpecification> statelessAsyncTools,
        List<McpStatelessServerFeatures.SyncToolSpecification> statelessSyncTools,
        PromptsConfiguration promptsConfiguration,
        PromptRegistry promptRegistry,
        List<McpServerFeatures.SyncPromptSpecification> syncPrompts,
        List<McpServerFeatures.AsyncPromptSpecification> asyncPrompts,
        List<McpStatelessServerFeatures.SyncPromptSpecification> statelessSyncPrompts,
        List<McpStatelessServerFeatures.AsyncPromptSpecification> statelessAsyncPrompts) {
        McpSchema.ServerCapabilities.Builder builder = McpSchema.ServerCapabilities.builder();
        if (CollectionUtils.isNotEmpty(syncTools) ||
            CollectionUtils.isNotEmpty(asyncTools) ||
            CollectionUtils.isNotEmpty(statelessAsyncTools) ||
            CollectionUtils.isNotEmpty(statelessSyncTools) ||
            toolRegistry.isNotEmpty()
        ) {
            builder.tools(toolsConfiguration.isListChanged());
        }
        if (CollectionUtils.isNotEmpty(syncPrompts) ||
            CollectionUtils.isNotEmpty(asyncPrompts) ||
            CollectionUtils.isNotEmpty(statelessSyncPrompts) ||
            CollectionUtils.isNotEmpty(statelessAsyncPrompts) ||
            promptRegistry.isNotEmpty()) {
            builder.prompts(promptsConfiguration.isListChanged());
        }
        if (CollectionUtils.isNotEmpty(resourceTemplates) ||
            CollectionUtils.isNotEmpty(resources) ||
            CollectionUtils.isNotEmpty(syncResources) ||
            CollectionUtils.isNotEmpty(asyncResources) ||
            CollectionUtils.isNotEmpty(statelessAsyncResources) ||
            CollectionUtils.isNotEmpty(statelessSyncResources) ||
            resourceRegistry.isNotEmpty()) {
            builder.resources(resourcesConfiguration.isSubscribe(), resourcesConfiguration.isListChanged());
        }
        if (CollectionUtils.isNotEmpty(syncCompletions) ||
            CollectionUtils.isNotEmpty(asyncCompletions) ||
            CollectionUtils.isNotEmpty(statelessAsyncCompletions) ||
            CollectionUtils.isNotEmpty(statelessSyncCompletions)) {
            builder.completions();
        }
        return builder;
    }

    @Prototype
    McpSchema.ServerCapabilities createServerCapabilities(McpSchema.ServerCapabilities.Builder builder) {
        return builder.build();
    }
}
