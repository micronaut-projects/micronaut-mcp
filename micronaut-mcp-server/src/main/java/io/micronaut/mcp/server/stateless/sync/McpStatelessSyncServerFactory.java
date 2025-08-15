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
package io.micronaut.mcp.server.stateless.sync;

import io.micronaut.context.annotation.Factory;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.mcp.server.conf.McpServerInfoConfiguration;
import io.micronaut.mcp.server.registry.ToolRegistry;
import io.micronaut.mcp.server.AbstractMcpServerFactory;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpStatelessServerFeatures;
import io.modelcontextprotocol.server.McpStatelessSyncServer;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpStatelessServerTransport;
import jakarta.inject.Singleton;

import java.util.List;

@Factory
@Internal
final class McpStatelessSyncServerFactory extends AbstractMcpServerFactory<McpServer.StatelessSyncSpecification, McpStatelessServerTransport,
    McpStatelessServerFeatures.SyncToolSpecification,
    McpStatelessServerFeatures.SyncCompletionSpecification,
    McpStatelessServerFeatures.SyncPromptSpecification,
    McpStatelessServerFeatures.SyncResourceSpecification> {
    @SuppressWarnings({"java:S107"})

    @Override
    protected List<McpStatelessServerFeatures.SyncToolSpecification> getRegistryTools(ToolRegistry toolRegistry) {
        return toolRegistry.getStatelessSyncToolSpecs();
    }

    @Override
    protected McpServer.StatelessSyncSpecification createMcpServerSpec(McpStatelessServerTransport transport,
                                                                       @Nullable McpServerInfoConfiguration configuration,
                                                                       McpSchema.ServerCapabilities capabilities,
                                                                       List<McpStatelessServerFeatures.SyncToolSpecification> tools,
                                                                       List<McpStatelessServerFeatures.SyncCompletionSpecification> completions,
                                                                       List<McpStatelessServerFeatures.SyncPromptSpecification> prompts,
                                                                       List<McpSchema.ResourceTemplate> resourceTemplates,
                                                                       List<McpStatelessServerFeatures.SyncResourceSpecification> resources) {
        McpServer.StatelessSyncSpecification spec = McpServer.sync(transport);
        if (configuration != null) {
            spec.serverInfo(configuration.getName(), configuration.getVersion());
        }
        spec.tools(tools);
        spec.completions(completions);
        spec.prompts(prompts);
        spec.resourceTemplates(resourceTemplates);
        spec.resources(resources);
        spec.capabilities(capabilities);
        return spec;
    }

    @Singleton
    McpStatelessSyncServer createMcpStatelessSyncServer(McpServer.StatelessSyncSpecification specification) {
        return specification.build();
    }

}
