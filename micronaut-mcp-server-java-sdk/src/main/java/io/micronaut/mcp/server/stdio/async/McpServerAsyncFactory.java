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
package io.micronaut.mcp.server.stdio.async;

import io.micronaut.context.annotation.Factory;
import io.micronaut.core.annotation.Internal;
import org.jspecify.annotations.Nullable;
import io.micronaut.mcp.server.AbstractMcpServerFactory;
import io.micronaut.mcp.conf.server.McpServerInfoConfiguration;
import io.micronaut.mcp.server.registry.CompletionRegistry;
import io.micronaut.mcp.server.registry.PromptRegistry;
import io.micronaut.mcp.server.registry.ResourceTemplateRegistry;
import io.micronaut.mcp.server.registry.ToolRegistry;
import io.micronaut.mcp.server.registry.ResourceRegistry;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.schema.JsonSchemaValidator;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpServerTransportProvider;
import jakarta.inject.Singleton;

import java.util.List;

@Factory
@Internal
final class McpServerAsyncFactory extends AbstractMcpServerFactory<McpServer.AsyncSpecification<?>,
    McpServerTransportProvider,
    McpServerFeatures.AsyncToolSpecification,
    McpServerFeatures.AsyncCompletionSpecification,
    McpServerFeatures.AsyncPromptSpecification,
    McpServerFeatures.AsyncResourceSpecification,
    McpServerFeatures.AsyncResourceTemplateSpecification> {

    @Singleton
    McpAsyncServer createMcpSyncServer(@SuppressWarnings("java:S3740") McpServer.AsyncSpecification<?> asyncSpecification) {
        return asyncSpecification.build();
    }

    @Override
    protected List<McpServerFeatures.AsyncToolSpecification> getTools(ToolRegistry toolRegistry) {
        return toolRegistry.getAsyncSpecs();
    }

    @Override
    protected List<McpServerFeatures.AsyncPromptSpecification> getPrompts(PromptRegistry promptRegistry) {
        return promptRegistry.getAsyncSpecs();
    }

    @Override
    protected List<McpServerFeatures.AsyncResourceSpecification> getResources(ResourceRegistry resourceRegistry) {
        return resourceRegistry.getAsyncSpecs();
    }

    @Override
    protected List<McpServerFeatures.AsyncResourceTemplateSpecification> getResourceTemplates(ResourceTemplateRegistry resourceTemplateRegistry) {
        return resourceTemplateRegistry.getAsyncSpecs();
    }

    @Override
    protected List<McpServerFeatures.AsyncCompletionSpecification> getCompletions(CompletionRegistry completionRegistry) {
        return completionRegistry.getAsyncSpecs();
    }

    @Override
    protected McpServer.AsyncSpecification<?> createMcpServerSpec(McpServerTransportProvider transport,
                                                                  McpJsonMapper jsonMapper,
                                                                  JsonSchemaValidator jsonSchemaValidator,
                                                                  @Nullable McpServerInfoConfiguration configuration,
                                                                  McpSchema.ServerCapabilities capabilities,
                                                                  List<McpServerFeatures.AsyncToolSpecification> tools,
                                                                  List<McpServerFeatures.AsyncCompletionSpecification> completions,
                                                                  List<McpServerFeatures.AsyncPromptSpecification> prompts,
                                                                  List<McpServerFeatures.AsyncResourceSpecification> resources,
                                                                  List<McpServerFeatures.AsyncResourceTemplateSpecification> resourceTemplates) {
        McpServer.AsyncSpecification<?> spec = McpServer.async(transport)
            .jsonMapper(jsonMapper)
            .jsonSchemaValidator(jsonSchemaValidator)
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
