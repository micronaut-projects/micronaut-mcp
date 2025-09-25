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
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpStatelessServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import reactor.core.publisher.Mono;

@Requires(beans = SearchTool.class)
@Factory
@Internal
final class SearchToolFactory {
    private static final String QUERY = "query";
    private final McpSchema.Tool mcpSearchTool;

    SearchToolFactory(SearchTool tool,
                      McpJsonMapper mcpJsonMapper,
                      JsonSchemaClassPathResourceLoader jsonSchemaClassPathResourceLoader) {
        String searchRequestJsonSchema = jsonSchemaClassPathResourceLoader.jsonSchemaStringForClass(SearchRequest.class)
                .orElseThrow(() -> new ConfigurationException("JSON Schema not found for SearchRequest"));
        String searchResponseJsonSchema = jsonSchemaClassPathResourceLoader.jsonSchemaStringForClass(SearchResponse.class)
                .orElseThrow(() -> new ConfigurationException("JSON Schema not found for SearchResponse"));
        this.mcpSearchTool = McpSchema.Tool.builder()
                .name(tool.getName())
                .description(tool.getDescription())
                .inputSchema(mcpJsonMapper, searchRequestJsonSchema)
                .outputSchema(mcpJsonMapper, searchResponseJsonSchema)
                .build();
    }

    @Requires(property = McpServerConfiguration.PROPERTY_REACTIVE, value = StringUtils.FALSE, defaultValue = StringUtils.FALSE)
    @Requires(property = McpServerConfiguration.PROPERTY_TRANSPORT, value = McpServerConfiguration.TRANSPORT_STDIO)
    @Named(SearchTool.DEFAULT_NAME)
    @Singleton
    McpServerFeatures.SyncToolSpecification searchToolSync(SearchTool searchTool) {
        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(mcpSearchTool)
                .callHandler((exchange, req) -> getCallToolResult(searchTool, req, exchange.transportContext())).build();
    }

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
        if (req.arguments() != null && req.arguments().get(QUERY) instanceof String query) {
            SearchResponse searchResponse = searchTool.search(new SearchRequest(query), mcpTransportContext);
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
