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

import io.micronaut.context.annotation.Prototype;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.mcp.conf.McpServerInfoConfiguration;
import io.micronaut.mcp.conf.PromptsConfiguration;
import io.micronaut.mcp.conf.ResourcesConfiguration;
import io.micronaut.mcp.conf.ToolsConfiguration;
import io.micronaut.mcp.server.registry.PromptRegistry;
import io.micronaut.mcp.server.registry.ToolRegistry;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Provider;

import java.util.List;

/**
 * An abstract factory class responsible for creating MCP server specifications.
 * This class provides a framework for deriving specific implementations of MCP server factories.
 *
 * @param <Spec> the type representing the MCP server specification.
 * @param <S>    the type representing the server transport.
 * @param <T>    the type representing tool specifications.
 * @param <C>    the type representing completion specifications.
 * @param <P>    the type representing prompt specifications.
 * @param <R>    the type representing resource specifications.
 */
@Internal
public abstract class AbstractMcpServerFactory<Spec, S, T, C, P, R> {

    /**
     * Retrieves a list of tools from the provided {@link ToolRegistry}.
     *
     * @param toolRegistry the registry of tools from which to retrieve the tools
     * @return a list of tools retrieved from the provided tool registry
     */
    protected abstract List<T> getTools(ToolRegistry toolRegistry);

    /**
     * Retrieves a list of prompts from the provided {@code PromptRegistry}.
     *
     * @param promptRegistry the registry of prompts from which to retrieve the prompts
     * @return a list of prompts retrieved from the provided prompt registry
     */
    protected abstract List<P> getPrompts(PromptRegistry promptRegistry);

    /**
     * Creates an MCP server specification based on the provided parameters.
     *
     * @param transport         The transport protocol used by the MCP server.
     * @param configuration     The configuration for the MCP server, or null if not provided.
     * @param capabilities      The server capabilities to be used for the MCP server.
     * @param tools             A list of tools to be included in the MCP server specification.
     * @param completions       A list of completion configurations for the MCP server.
     * @param prompts           A list of prompt configurations for the MCP server.
     * @param resourceTemplates A list of resource templates for the MCP server.
     * @param resources         A list of server resources to be defined in the specification.
     * @return The constructed server specification.
     */
    protected abstract Spec createMcpServerSpec(S transport,
                                                @Nullable McpServerInfoConfiguration configuration,
                                                McpSchema.ServerCapabilities capabilities,
                                                List<T> tools,
                                                List<C> completions,
                                                List<P> prompts,
                                                List<McpSchema.ResourceTemplate> resourceTemplates,
                                                List<R> resources);

    @SuppressWarnings({"java:S107", "ParameterNumber"})
    @Prototype
    final Spec buildMcpServerSpec(S transport,
                                  @Nullable McpServerInfoConfiguration configuration,
                                  ToolsConfiguration toolsConfiguration,
                                  PromptsConfiguration promptsConfiguration,
                                  ResourcesConfiguration resourcesConfiguration,
                                  McpSchema.ServerCapabilities.Builder capabilitiesBuilder,
                                  Provider<McpSchema.ServerCapabilities> capabilitiesProvider,
                                  ToolRegistry toolRegistry,
                                  PromptRegistry promptRegistry,
                                  List<T> tools,
                                  List<C> completions,
                                  List<P> prompts,
                                  List<McpSchema.ResourceTemplate> resourceTemplates,
                                  List<R> resources) {
        List<T> allTools = CollectionUtils.concat(tools, getTools(toolRegistry));
        if (!allTools.isEmpty()) {
            capabilitiesBuilder.tools(toolsConfiguration.isListChanged());
        }
        List<P> allPrompts = CollectionUtils.concat(prompts, getPrompts(promptRegistry));
        if (!allPrompts.isEmpty()) {
            capabilitiesBuilder.prompts(promptsConfiguration.isListChanged());
        }
        if (!resourceTemplates.isEmpty() || !resources.isEmpty()) {
            capabilitiesBuilder.resources(resourcesConfiguration.isSubscribe(), resourcesConfiguration.isListChanged());
        }
        if (!completions.isEmpty()) {
            capabilitiesBuilder.completions();
        }
        return createMcpServerSpec(transport, configuration, capabilitiesProvider.get(), allTools, completions, allPrompts, resourceTemplates, resources);
    }

}
