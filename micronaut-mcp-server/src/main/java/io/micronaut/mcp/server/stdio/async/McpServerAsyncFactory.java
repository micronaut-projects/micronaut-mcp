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
import io.micronaut.core.annotation.Nullable;
import io.micronaut.mcp.server.AbstractMcpServerFactory;
import io.micronaut.mcp.server.conf.McpServerInfoConfiguration;
import io.micronaut.mcp.server.processor.ToolRegistry;
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
    McpServerFeatures.AsyncResourceSpecification> {

    @Singleton
    McpAsyncServer createMcpSyncServer(@SuppressWarnings("java:S3740") McpServer.AsyncSpecification<?> asyncSpecification) {
        return asyncSpecification.build();
    }

    @Override
    protected List<McpServerFeatures.AsyncToolSpecification> getRegistryTools(ToolRegistry toolRegistry) {
        return toolRegistry.getAsyncToolSpecs();
    }

    @Override
    protected McpServer.AsyncSpecification<?> createMcpServerSpec(McpServerTransportProvider transport,
                                                                  @Nullable McpServerInfoConfiguration configuration,
                                                                  McpSchema.ServerCapabilities capabilities,
                                                                  List<McpServerFeatures.AsyncToolSpecification> tools,
                                                                  List<McpServerFeatures.AsyncCompletionSpecification> completions,
                                                                  List<McpServerFeatures.AsyncPromptSpecification> prompts,
                                                                  List<McpSchema.ResourceTemplate> resourceTemplates,
                                                                  List<McpServerFeatures.AsyncResourceSpecification> resources) {
        McpServer.AsyncSpecification<?> spec = McpServer.async(transport)
            .capabilities(capabilities);
        if (configuration != null) {
            spec.serverInfo(configuration.getName(), configuration.getVersion());
        }
        spec.tools(tools);
        spec.completions(completions);
        spec.prompts(prompts);
        spec.resourceTemplates(resourceTemplates);
        spec.resources(resources);
        return spec;
    }
}
