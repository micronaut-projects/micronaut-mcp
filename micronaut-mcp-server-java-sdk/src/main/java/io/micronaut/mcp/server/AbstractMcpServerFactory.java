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
import org.jspecify.annotations.Nullable;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.mcp.conf.server.McpServerInfoConfiguration;
import io.micronaut.mcp.conf.server.PromptsConfiguration;
import io.micronaut.mcp.conf.server.ResourcesConfiguration;
import io.micronaut.mcp.conf.server.ToolsConfiguration;
import io.micronaut.mcp.server.registry.CompletionRegistry;
import io.micronaut.mcp.server.registry.PromptRegistry;
import io.micronaut.mcp.server.registry.ResourceRegistry;
import io.micronaut.mcp.server.registry.ResourceTemplateRegistry;
import io.micronaut.mcp.server.registry.ToolRegistry;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.schema.JsonSchemaValidator;
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
 * @param <U>    the type representing resource template specifications.
 */
@Internal
public abstract class AbstractMcpServerFactory<Spec, S, T, C, P, R, U> {

    /**
     * Retrieves a list of tools from the provided {@link ToolRegistry}.
     *
     * @param toolRegistry the registry of tools from which to retrieve the tools
     * @return a list of tools retrieved from the provided tool registry
     */
    protected abstract List<T> getTools(ToolRegistry toolRegistry);

    /**
     * Retrieves a list of prompts from the provided {@link PromptRegistry}.
     *
     * @param promptRegistry the registry of prompts from which to retrieve the prompts
     * @return a list of prompts retrieved from the provided prompt registry
     */
    protected abstract List<P> getPrompts(PromptRegistry promptRegistry);

    /**
     * Retrieves a list of resources from the provided {@link ResourceRegistry}.
     *
     * @param resourceRegistry the registry of resources from which to retrieve the resources
     * @return a list of resources retrieved from the provided resource registry
     */
    protected abstract List<R> getResources(ResourceRegistry resourceRegistry);

    /**
     * Retrieves a list of resources templates from the provided {@link ResourceTemplateRegistry}.
     *
     * @param resourceTemplateRegistry the registry of resource templates from which to retrieve the resource templates.
     * @return a list of resources templates retrieved from the provided resource registry
     */
    protected abstract List<U> getResourceTemplates(ResourceTemplateRegistry resourceTemplateRegistry);

    /**
     * Retrieves a list of completions from the provided {@link CompletionRegistry}.
     *
     * @param completionRegistry the registry of completions from which to retrieve the completions.
     * @return a list of completions retrieved from the provided resource registry
     */
    protected abstract List<C> getCompletions(CompletionRegistry completionRegistry);

    /**
     * Creates an MCP server specification based on the provided parameters.
     *
     * @param transport         The transport protocol used by the MCP server.
     * @param jsonMapper        MCP JSON Mapper
     * @param jsonSchemaValidator MCP JSON schema validator
     * @param configuration     The configuration for the MCP server, or null if not provided.
     * @param capabilities      The server capabilities to be used for the MCP server.
     * @param tools             A list of tools to be included in the MCP server specification.
     * @param completions       A list of completion configurations for the MCP server.
     * @param prompts           A list of prompt configurations for the MCP server.
     * @param resources         A list of server resources to be defined in the specification.
     * @param resourcesTemplates A list of resource templates for the MCP server.
     * @return the MCP server specification.
     */
    protected abstract Spec createMcpServerSpec(S transport,
                                                McpJsonMapper jsonMapper,
                                                JsonSchemaValidator jsonSchemaValidator,
                                                @Nullable McpServerInfoConfiguration configuration,
                                                McpSchema.ServerCapabilities capabilities,
                                                List<T> tools,
                                                List<C> completions,
                                                List<P> prompts,
                                                List<R> resources,
                                                List<U> resourcesTemplates);

    /**
     *
     * @param transport Transport
     * @param jsonMapper MCP JSON Mapper
     * @param jsonSchemaValidator MCP JSON schema validator
     * @param configuration MCP Server Info Configuration
     * @param toolsConfiguration Tools Configuration
     * @param promptsConfiguration Prompts Configuration
     * @param resourcesConfiguration Resources Configuration
     * @param capabilitiesBuilder Capabilities Builder
     * @param capabilitiesProvider Capabilities Provider
     * @param toolRegistry Tool Registry
     * @param promptRegistry Prompt Registry
     * @param resourceRegistry Resource Registry
     * @param resourceTemplateRegistry Resource Template Registry
     * @param completionRegistry Completion Registry
     * @param tools Tools
     * @param completions Completions
     * @param prompts Prompts
     * @param resourceTemplates Resource Templates
     * @param resources Resources
     * @return the MCP server specification.
     */
    @SuppressWarnings({"java:S107", "ParameterNumber"})
    @Prototype
    // keep the visibility modifier public for GraalVM
    public Spec buildMcpServerSpec(S transport,
                                   McpJsonMapper jsonMapper,
                                   JsonSchemaValidator jsonSchemaValidator,
                                   @Nullable McpServerInfoConfiguration configuration,
                                  ToolsConfiguration toolsConfiguration,
                                  PromptsConfiguration promptsConfiguration,
                                  ResourcesConfiguration resourcesConfiguration,
                                  McpSchema.ServerCapabilities.Builder capabilitiesBuilder,
                                  Provider<McpSchema.ServerCapabilities> capabilitiesProvider,
                                  ToolRegistry toolRegistry,
                                  PromptRegistry promptRegistry,
                                  ResourceRegistry resourceRegistry,
                                  ResourceTemplateRegistry resourceTemplateRegistry,
                                  CompletionRegistry completionRegistry,
                                  List<T> tools,
                                  List<C> completions,
                                  List<P> prompts,
                                  List<R> resources,
                                   List<U> resourceTemplates) {
        List<T> allTools = CollectionUtils.concat(tools, getTools(toolRegistry));
        if (!allTools.isEmpty()) {
            capabilitiesBuilder.tools(toolsConfiguration.isListChanged());
        }
        List<P> allPrompts = CollectionUtils.concat(prompts, getPrompts(promptRegistry));
        if (!allPrompts.isEmpty()) {
            capabilitiesBuilder.prompts(promptsConfiguration.isListChanged());
        }
        List<R> allResources = CollectionUtils.concat(resources, getResources(resourceRegistry));
        List<U> allResourceTemplates = CollectionUtils.concat(resourceTemplates, getResourceTemplates(resourceTemplateRegistry));
        if (!allResourceTemplates.isEmpty() || !allResources.isEmpty()) {
            capabilitiesBuilder.resources(resourcesConfiguration.isSubscribe(), resourcesConfiguration.isListChanged());
        }
        List<C> allCompletions = CollectionUtils.concat(completions, getCompletions(completionRegistry));
        if (!allCompletions.isEmpty()) {
            capabilitiesBuilder.completions();
        }
        return createMcpServerSpec(transport, jsonMapper, jsonSchemaValidator, configuration, capabilitiesProvider.get(), allTools, allCompletions, allPrompts, allResources, allResourceTemplates);
    }

}
