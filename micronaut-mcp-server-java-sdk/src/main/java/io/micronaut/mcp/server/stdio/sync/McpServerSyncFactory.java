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
package io.micronaut.mcp.server.stdio.sync;

import io.micronaut.context.annotation.Factory;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.mcp.server.AbstractMcpServerFactory;
import io.micronaut.mcp.conf.McpServerInfoConfiguration;
import io.micronaut.mcp.server.registry.PromptRegistry;
import io.micronaut.mcp.server.registry.ToolRegistry;
import io.micronaut.mcp.server.registry.ResourceRegistry;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpServerTransportProvider;
import jakarta.inject.Singleton;

import java.util.List;

@Internal
@Factory
final class McpServerSyncFactory extends AbstractMcpServerFactory<McpServer.SyncSpecification<?>,
    McpServerTransportProvider,
    McpServerFeatures.SyncToolSpecification,
    McpServerFeatures.SyncCompletionSpecification,
    McpServerFeatures.SyncPromptSpecification,
    McpServerFeatures.SyncResourceSpecification> {

    @Singleton
    McpSyncServer createMcpSyncServer(McpServer.SyncSpecification<?> syncSpecification) {
        return syncSpecification.build();
    }

    @Override
    protected List<McpServerFeatures.SyncToolSpecification> getTools(ToolRegistry toolRegistry) {
        return toolRegistry.getSyncSpecs();
    }

    @Override
    protected List<McpServerFeatures.SyncPromptSpecification> getPrompts(PromptRegistry promptRegistry) {
        return promptRegistry.getSyncSpecs();
    }

    @Override
    protected List<McpServerFeatures.SyncResourceSpecification> getResources(ResourceRegistry resourceRegistry) {
        return resourceRegistry.getSyncSpecs();
    }

    @Override
    protected McpServer.SyncSpecification<?> createMcpServerSpec(McpServerTransportProvider transport,
                                                                 @Nullable McpServerInfoConfiguration configuration,
                                                                 McpSchema.ServerCapabilities capabilities,
                                                                 List<McpServerFeatures.SyncToolSpecification> tools,
                                                                 List<McpServerFeatures.SyncCompletionSpecification> completions,
                                                                 List<McpServerFeatures.SyncPromptSpecification> prompts,
                                                                 List<McpSchema.ResourceTemplate> resourceTemplates,
                                                                 List<McpServerFeatures.SyncResourceSpecification> resources) {
        McpServer.SyncSpecification<?> spec = McpServer.sync(transport)
            .capabilities(capabilities)
            .tools(tools)
            .completions(completions)
            .prompts(prompts)
            .resourceTemplates(resourceTemplates)
            .resources(resources);
        if (configuration != null) {
            spec.serverInfo(configuration.getName(), configuration.getVersion());
        }
        return spec;
    }
}
