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
package io.micronaut.mcp.server.stateless.async;

import io.micronaut.context.annotation.Factory;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.mcp.conf.McpServerInfoConfiguration;
import io.micronaut.mcp.server.AbstractMcpServerFactory;
import io.micronaut.mcp.server.registry.PromptRegistry;
import io.micronaut.mcp.server.registry.ToolRegistry;
import io.micronaut.mcp.server.registry.ResourceRegistry;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpStatelessAsyncServer;
import io.modelcontextprotocol.server.McpStatelessServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpStatelessServerTransport;
import jakarta.inject.Singleton;

import java.util.List;

/**
 * Factory to instantiate {@link McpServer.StatelessAsyncSpecification} and {@link McpStatelessAsyncServer}.
 */
@Factory
@Internal
final class McpStatelessAsyncServerFactory extends AbstractMcpServerFactory<McpServer.StatelessAsyncSpecification,
    McpStatelessServerTransport,
    McpStatelessServerFeatures.AsyncToolSpecification,
    McpStatelessServerFeatures.AsyncCompletionSpecification,
    McpStatelessServerFeatures.AsyncPromptSpecification,
    McpStatelessServerFeatures.AsyncResourceSpecification> {

    @Override
    protected List<McpStatelessServerFeatures.AsyncToolSpecification> getTools(ToolRegistry toolRegistry) {
        return toolRegistry.getStatelessAsyncSpecs();
    }

    @Override
    protected List<McpStatelessServerFeatures.AsyncPromptSpecification> getPrompts(PromptRegistry promptRegistry) {
        return promptRegistry.getStatelessAsyncSpecs();
    }

    @Override
    protected List<McpStatelessServerFeatures.AsyncResourceSpecification> getResources(ResourceRegistry resourceRegistry) {
        return resourceRegistry.getStatelessAsyncSpecs();
    }

    @Override
    protected McpServer.StatelessAsyncSpecification createMcpServerSpec(McpStatelessServerTransport transport,
                                                                        @Nullable McpServerInfoConfiguration configuration,
                                                                        McpSchema.ServerCapabilities capabilities,
                                                                        List<McpStatelessServerFeatures.AsyncToolSpecification> tools,
                                                                        List<McpStatelessServerFeatures.AsyncCompletionSpecification> completions,
                                                                        List<McpStatelessServerFeatures.AsyncPromptSpecification> prompts,
                                                                        List<McpSchema.ResourceTemplate> resourceTemplates,
                                                                        List<McpStatelessServerFeatures.AsyncResourceSpecification> resources) {
        McpServer.StatelessAsyncSpecification spec = McpServer.async(transport)
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

    @Singleton
    McpStatelessAsyncServer createMcpStatelessSyncServer(McpServer.StatelessAsyncSpecification specification) {
        return specification.build();
    }
}
