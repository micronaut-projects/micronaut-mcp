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
package io.micronaut.mcp.server.tools.fetch;

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

import java.util.Optional;

@Requires(beans = FetchTool.class)
@Factory
@Internal
final class FetchToolFactory {
    private static final String ID = "id";
    private final McpSchema.Tool mcpFetchTool;

    FetchToolFactory(FetchTool tool,
                     McpJsonMapper mcpJsonMapper,
                     JsonSchemaClassPathResourceLoader jsonSchemaClassPathResourceLoader) {
        this.mcpFetchTool = McpSchema.Tool.builder()
            .name(tool.getName())
            .title(tool.getTitle())
            .description(tool.getDescription())
            .inputSchema(mcpJsonMapper, jsonSchemaClassPathResourceLoader.jsonSchemaStringForClass(FetchRequest.class)
                .orElseThrow(() -> new ConfigurationException("JSON Schema not found for FetchRequest")))
            .outputSchema(mcpJsonMapper, jsonSchemaClassPathResourceLoader.jsonSchemaStringForClass(FetchResponse.class)
                .orElseThrow(() -> new ConfigurationException("JSON Schema not found for FetchResponse")))
            .build();
    }

    @Requires(property = McpServerConfiguration.PROPERTY_REACTIVE, value = StringUtils.FALSE, defaultValue = StringUtils.FALSE)
    @Requires(property = McpServerConfiguration.PROPERTY_TRANSPORT, value = McpServerConfiguration.TRANSPORT_STDIO)
    @Singleton
    @Named(FetchTool.DEFAULT_NAME)
    McpServerFeatures.SyncToolSpecification fetchToolSync(FetchTool fetchTool) {
        return McpServerFeatures.SyncToolSpecification.builder()
            .tool(mcpFetchTool)
            .callHandler((exchange, req) ->
                    getCallToolResult(fetchTool, req, exchange.transportContext())).build();
    }

    @Requires(property = McpServerConfiguration.PROPERTY_REACTIVE, value = StringUtils.TRUE)
    @Requires(property = McpServerConfiguration.PROPERTY_TRANSPORT, value = McpServerConfiguration.TRANSPORT_STDIO)
    @Singleton
    @Named(FetchTool.DEFAULT_NAME)
    McpServerFeatures.AsyncToolSpecification fetchToolAsync(FetchTool fetchTool) {
        return McpServerFeatures.AsyncToolSpecification.builder()
            .tool(mcpFetchTool)
            .callHandler((exchange, req) ->
                Mono.just(getCallToolResult(fetchTool, req, exchange.transportContext())))
            .build();
    }

    @Requires(property = McpServerConfiguration.PROPERTY_REACTIVE, value = StringUtils.TRUE)
    @Requires(property = McpServerConfiguration.PROPERTY_TRANSPORT, value = McpServerConfiguration.TRANSPORT_HTTP)
    @Singleton
    @Named(FetchTool.DEFAULT_NAME)
    McpStatelessServerFeatures.AsyncToolSpecification fetchToolStatelessAsync(FetchTool fetchTool) {
        return McpStatelessServerFeatures.AsyncToolSpecification.builder()
            .tool(mcpFetchTool)
            .callHandler((exchange, req) ->
                Mono.just(getCallToolResult(fetchTool, req, exchange)))
            .build();
    }

    @Requires(property = McpServerConfiguration.PROPERTY_REACTIVE, value = StringUtils.FALSE, defaultValue = StringUtils.FALSE)
    @Requires(property = McpServerConfiguration.PROPERTY_TRANSPORT, value = McpServerConfiguration.TRANSPORT_HTTP)
    @Singleton
    @Named(FetchTool.DEFAULT_NAME)
    McpStatelessServerFeatures.SyncToolSpecification fetchToolStatelessSync(FetchTool fetchTool) {
        return McpStatelessServerFeatures.SyncToolSpecification.builder()
            .tool(mcpFetchTool)
            .callHandler((exchange, req) ->
                getCallToolResult(fetchTool, req, exchange))
            .build();
    }

    private static McpSchema.CallToolResult getCallToolResult(FetchTool fetchTool,
                                                              McpSchema.CallToolRequest req,
                                                              McpTransportContext mcpTransportContext) {
        Object obj = req.arguments().get(ID);
        if (obj instanceof String id) {
            Optional<FetchResponse> fetchResponse = fetchTool.fetch(new FetchRequest(id), mcpTransportContext);
            if (fetchResponse.isEmpty()) {
                return McpSchema.CallToolResult.builder()
                        .addTextContent("search result not found")
                        .isError(true)
                        .build();
            }
            return McpSchema.CallToolResult.builder()
                .structuredContent(fetchResponse.get())
                .isError(false)
                .build();
        } else {
            return McpSchema.CallToolResult.builder()
                .isError(true)
                .build();
        }
    }
}
