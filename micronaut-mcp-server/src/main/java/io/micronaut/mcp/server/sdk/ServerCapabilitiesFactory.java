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
package io.micronaut.mcp.server.sdk;

import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Prototype;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.util.CollectionUtils;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpStatelessServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;

import java.util.List;
import java.util.Map;

@Internal
@Factory
class ServerCapabilitiesFactory {
    @SuppressWarnings("ParameterNumber")
    @Prototype
    McpSchema.ServerCapabilities.Builder createServerCapabilitiesBuilder(
        List<McpServerFeatures.SyncCompletionSpecification> syncCompletions,
        List<McpServerFeatures.AsyncCompletionSpecification> asyncCompletions,
        List<McpStatelessServerFeatures.AsyncCompletionSpecification> statelessAsyncCompletions,
        List<McpStatelessServerFeatures.SyncCompletionSpecification> statelessSyncCompletions,
        List<McpSchema.ResourceTemplate> resourceTemplates,
        List<McpSchema.Resource> resources,
        List<McpServerFeatures.SyncResourceSpecification> syncResources,
        List<McpStatelessServerFeatures.AsyncResourceSpecification> statelessAsyncResources,
        List<McpStatelessServerFeatures.SyncResourceSpecification> statelessSyncResources,
        List<McpServerFeatures.SyncToolSpecification> syncTools,
        List<McpServerFeatures.AsyncToolSpecification> asyncTools,
        List<McpStatelessServerFeatures.AsyncToolSpecification> statelessAsyncTools,
        List<McpStatelessServerFeatures.SyncToolSpecification> statelessSyncTools,
        List<McpServerFeatures.SyncPromptSpecification> syncPrompts,
        List<McpServerFeatures.AsyncPromptSpecification> asyncPrompts,
        List<McpStatelessServerFeatures.SyncPromptSpecification> statelessSyncPrompts,
        List<McpStatelessServerFeatures.AsyncPromptSpecification> statelessAsyncPrompts) {
        McpSchema.ServerCapabilities.Builder builder = McpSchema.ServerCapabilities.builder();
        if (CollectionUtils.isNotEmpty(syncTools) || CollectionUtils.isNotEmpty(asyncTools) || CollectionUtils.isNotEmpty(statelessAsyncTools) || CollectionUtils.isNotEmpty(statelessSyncTools)) {
            builder.tools(true); // should listChanged be set to true?
        }
        if (CollectionUtils.isNotEmpty(syncPrompts) || CollectionUtils.isNotEmpty(asyncPrompts) || CollectionUtils.isNotEmpty(statelessSyncPrompts) || CollectionUtils.isNotEmpty(statelessAsyncPrompts)) {
            builder.prompts(true); // should listChanged be set to true?
        }
        if (CollectionUtils.isNotEmpty(resourceTemplates) || CollectionUtils.isNotEmpty(resources) || CollectionUtils.isNotEmpty(syncResources) || CollectionUtils.isNotEmpty(statelessAsyncResources) || CollectionUtils.isNotEmpty(statelessSyncResources)) {
            builder.resources(true, true); // should subscribe and listChanged be set to true?
        }
        if (CollectionUtils.isNotEmpty(syncCompletions) || CollectionUtils.isNotEmpty(asyncCompletions) || CollectionUtils.isNotEmpty(statelessAsyncCompletions) || CollectionUtils.isNotEmpty(statelessSyncCompletions)) {
            builder.completions();
        }
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

        //return serverCapabilities;
    }
}
