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
package io.micronaut.mcp.server.tools.search;

import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.exceptions.ConfigurationException;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.util.StringUtils;
import io.micronaut.jsonschema.utils.JsonSchemaClassPathResourceLoader;
import io.micronaut.mcp.conf.McpServerConfiguration;
import io.micronaut.mcp.server.context.MicronautMcpTransportContext;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpStatelessServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import reactor.core.publisher.Mono;

/**
 * Factory to create beans of type {@link McpServerFeatures.SyncToolSpecification}, {@link McpServerFeatures.AsyncToolSpecification}, {@link McpStatelessServerFeatures.SyncToolSpecification}, and {@link McpStatelessServerFeatures.AsyncToolSpecification} given a bean of type {@link SearchTool}.
 */
@Requires(beans = SearchTool.class)
@Factory
@Internal
final class SearchToolFactory {
    private static final String QUERY = "query";
    private final McpSchema.Tool mcpSearchTool;

    SearchToolFactory(SearchTool tool,
                      McpJsonMapper mcpJsonMapper,
                      JsonSchemaClassPathResourceLoader jsonSchemaClassPathResourceLoader) {
        this.mcpSearchTool = McpSchema.Tool.builder()
                .name(tool.getName())
                .title(tool.getTitle())
                .description(tool.getDescription())
                .inputSchema(mcpJsonMapper, jsonSchemaClassPathResourceLoader.jsonSchemaStringForClass(SearchRequest.class)
                    .orElseThrow(() -> new ConfigurationException("JSON Schema not found for SearchRequest")))
                .outputSchema(mcpJsonMapper, jsonSchemaClassPathResourceLoader.jsonSchemaStringForClass(SearchResponse.class)
                    .orElseThrow(() -> new ConfigurationException("JSON Schema not found for SearchResponse")))
                .build();
    }

    /**
     * Creates a bean of type {@link McpServerFeatures.SyncToolSpecification} for a bean of type {@link SearchTool}.
     * The method requires the property {@link McpServerConfiguration#PROPERTY_REACTIVE} not to be set to true.
     * The method requires the property {@link McpServerConfiguration#PROPERTY_TRANSPORT} be set to STDIO.
     * @param searchTool Search Tool
     * @return Bean of type {@link McpServerFeatures.SyncToolSpecification}
     */
    @Requires(property = McpServerConfiguration.PROPERTY_REACTIVE, value = StringUtils.FALSE, defaultValue = StringUtils.FALSE)
    @Requires(property = McpServerConfiguration.PROPERTY_TRANSPORT, value = McpServerConfiguration.TRANSPORT_STDIO)
    @Named(SearchTool.DEFAULT_NAME)
    @Singleton
    McpServerFeatures.SyncToolSpecification searchToolSync(SearchTool searchTool) {
        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(mcpSearchTool)
                .callHandler((exchange, req) -> getCallToolResult(searchTool, req, exchange.transportContext())).build();
    }

    /**
     * Creates a bean of type {@link McpServerFeatures.AsyncToolSpecification} for a bean of type {@link SearchTool}.
     * The method requires the property {@link McpServerConfiguration#PROPERTY_REACTIVE} to be set to true.
     * The method requires the property {@link McpServerConfiguration#PROPERTY_TRANSPORT} be set to STDIO.
     * @param searchTool Search Tool
     * @return Bean of type {@link McpServerFeatures.AsyncToolSpecification}
     */
    @Requires(property = McpServerConfiguration.PROPERTY_REACTIVE, value = StringUtils.TRUE)
    @Requires(property = McpServerConfiguration.PROPERTY_TRANSPORT, value = McpServerConfiguration.TRANSPORT_STDIO)
    @Named(SearchTool.DEFAULT_NAME)
    @Singleton
    McpServerFeatures.AsyncToolSpecification searchToolAsync(SearchTool searchTool) {
        return McpServerFeatures.AsyncToolSpecification.builder()
                .tool(mcpSearchTool)
                .callHandler((exchange, req) ->
                        Mono.just(getCallToolResult(searchTool, req, exchange.transportContext())))
                .build();
    }

    /**
     * Creates a bean of type {@link McpStatelessServerFeatures.AsyncToolSpecification} for a bean of type {@link SearchTool}.
     * The method requires the property {@link McpServerConfiguration#PROPERTY_REACTIVE} to be set to true.
     * The method requires the property {@link McpServerConfiguration#PROPERTY_TRANSPORT} be set to HTTP.
     * @param searchTool Search Tool
     * @return Bean of type {@link McpStatelessServerFeatures.AsyncToolSpecification}
     */
    @Requires(property = McpServerConfiguration.PROPERTY_REACTIVE, value = StringUtils.TRUE)
    @Requires(property = McpServerConfiguration.PROPERTY_TRANSPORT, value = McpServerConfiguration.TRANSPORT_HTTP)
    @Named(SearchTool.DEFAULT_NAME)
    @Singleton
    McpStatelessServerFeatures.AsyncToolSpecification searchToolStatelessAsync(SearchTool searchTool) {
        return McpStatelessServerFeatures.AsyncToolSpecification.builder()
                .tool(mcpSearchTool)
                .callHandler((exchange, req) ->
                        Mono.just(getCallToolResult(searchTool, req, exchange)))
                .build();
    }

    /**
     * Creates a bean of type {@link McpStatelessServerFeatures.SyncToolSpecification} for a bean of type {@link SearchTool}.
     * The method requires the property {@link McpServerConfiguration#PROPERTY_REACTIVE} not to be set to true.
     * The method requires the property {@link McpServerConfiguration#PROPERTY_TRANSPORT} be set to HTTP.
     * @param searchTool Search Tool
     * @return Bean of type {@link McpStatelessServerFeatures.SyncToolSpecification}
     */
    @Requires(property = McpServerConfiguration.PROPERTY_REACTIVE, value = StringUtils.FALSE, defaultValue = StringUtils.FALSE)
    @Requires(property = McpServerConfiguration.PROPERTY_TRANSPORT, value = McpServerConfiguration.TRANSPORT_HTTP)
    @Named(SearchTool.DEFAULT_NAME)
    @Singleton
    McpStatelessServerFeatures.SyncToolSpecification searchToolStatelessSync(SearchTool searchTool) {
        return McpStatelessServerFeatures.SyncToolSpecification.builder()
                .tool(mcpSearchTool)
                .callHandler((exchange, req) ->
                        getCallToolResult(searchTool, req, exchange))
                .build();
    }

    private static McpSchema.CallToolResult getCallToolResult(SearchTool searchTool,
                                                              McpSchema.CallToolRequest req,
                                                              McpTransportContext mcpTransportContext) {
        if (req.arguments() != null && req.arguments().get(QUERY) instanceof String query && mcpTransportContext instanceof MicronautMcpTransportContext context) {
            SearchResponse searchResponse = searchTool.search(new SearchRequest(query), context);
            return McpSchema.CallToolResult.builder()
                    .structuredContent(searchResponse)
                    .build();
        } else {
            return McpSchema.CallToolResult.builder()
                    .isError(true)
                    .build();
        }
    }
}
