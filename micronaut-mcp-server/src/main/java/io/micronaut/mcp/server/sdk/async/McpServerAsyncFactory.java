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
package io.micronaut.mcp.server.sdk.async;

import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Prototype;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.mcp.server.sdk.conf.McpServerInfoConfiguration;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpServerTransportProvider;
import jakarta.inject.Singleton;

import java.util.List;

@Factory
@Internal
class McpServerAsyncFactory {
    @Prototype
    McpSchema.ServerCapabilities.Builder createServerCapabilitiesBuilder(List<McpServerFeatures.AsyncToolSpecification> asyncToolSpecifications,
                                                                         List<McpServerFeatures.AsyncCompletionSpecification> asyncCompletionsSpecifications,
                                                                         List<McpServerFeatures.AsyncPromptSpecification> asyncPromptSpecifications,
                                                                         List<McpSchema.ResourceTemplate> resourceTemplates,
                                                                         List<McpSchema.Resource> resources) {
        McpSchema.ServerCapabilities.Builder builder = McpSchema.ServerCapabilities.builder();
        if (CollectionUtils.isNotEmpty(asyncToolSpecifications)) {
            builder.tools(true); // should listChanged be set to true?
        }
        if (CollectionUtils.isNotEmpty(asyncCompletionsSpecifications)) {
            builder.completions();
        }
        if (CollectionUtils.isNotEmpty(asyncPromptSpecifications)) {
            builder.prompts(true); // should listChanged be set to true?
        }
        if (CollectionUtils.isNotEmpty(resourceTemplates) || CollectionUtils.isNotEmpty(resources)) {
            builder.resources(true, true); // should subscribe and listChanged be set to true?
        }
        builder.logging();
        return builder;
    }

    @Singleton
    McpSchema.ServerCapabilities createServerCapabilities(McpSchema.ServerCapabilities.Builder builder) {
        return builder.build();
    }

    @Prototype
    @Singleton
    McpServer.AsyncSpecification createMcpServerSyncSpecification(McpServerTransportProvider mcpServerTransportProvider,
                                                                 @Nullable McpServerInfoConfiguration mcpServerInfoConfiguration,
                                                                 McpSchema.ServerCapabilities mcpServerCapabilities,
                                                                 List<McpServerFeatures.AsyncToolSpecification> asyncToolSpecifications,
                                                                 List<McpServerFeatures.AsyncCompletionSpecification> asyncCompletionsSpecifications,
                                                                 List<McpServerFeatures.AsyncPromptSpecification> asyncPromptSpecifications,
                                                                 List<McpSchema.ResourceTemplate> resourceTemplates,
                                                                  List<McpSchema.Resource> resources) {
        McpServer.AsyncSpecification spec = McpServer.async(mcpServerTransportProvider)
            .capabilities(mcpServerCapabilities);
        if (mcpServerInfoConfiguration != null) {
            spec.serverInfo(mcpServerInfoConfiguration.getName(), mcpServerInfoConfiguration.getVersion());
        }
        if (CollectionUtils.isNotEmpty(asyncToolSpecifications)) {
            spec.tools(asyncToolSpecifications);
        }
        if (CollectionUtils.isNotEmpty(asyncCompletionsSpecifications)) {
            spec.completions(asyncCompletionsSpecifications);
        }
        if (CollectionUtils.isNotEmpty(asyncPromptSpecifications)) {
            spec.prompts(asyncPromptSpecifications);
        }
        if (CollectionUtils.isNotEmpty(resourceTemplates)) {
            spec.resourceTemplates(resourceTemplates);
        }
        if (CollectionUtils.isNotEmpty(resources)) {
            spec.resources(resources);
        }
        return spec;
    }

    @Singleton
    McpAsyncServer createMcpSyncServer(McpServer.AsyncSpecification asyncSpecification) {
        return asyncSpecification.build();
    }
}
