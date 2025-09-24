package io.micronaut.mcp.server.chatgpt;

import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.Internal;
import io.micronaut.json.JsonMapper;
import io.micronaut.jsonschema.utils.JsonSchemaClassPathResourceLoader;
import io.micronaut.mcp.chatgpt.SearchTool;
import io.micronaut.mcp.chatgpt.SearchToolResult;
import io.micronaut.mcp.chatgpt.SearchToolResults;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpStatelessServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Singleton;
import reactor.core.publisher.Mono;
import java.util.Map;
import java.util.List;

@Factory
@Internal
final class SearchToolFactory {

    private static final String QUERY = "query";
    public static final McpSchema.JsonSchema SEARCH_TOOL_INPUT_SCHEMA = new McpSchema.JsonSchema("object",
        Map.of(QUERY, Map.of("type", "string", "minLength", 1, "description", "user's search query")),
        List.of(QUERY),
        null,
        null,
        null);
    private final JsonSchemaClassPathResourceLoader jsonSchemaClassPathResourceLoader;
    private final McpJsonMapper mcpJsonMapper;

    SearchToolFactory(McpJsonMapper mcpJsonMapper,
                      JsonSchemaClassPathResourceLoader jsonSchemaClassPathResourceLoader) {
        this.mcpJsonMapper = mcpJsonMapper;
        this.jsonSchemaClassPathResourceLoader = jsonSchemaClassPathResourceLoader;
    }

    @Requires(beans = SearchTool.class)
    @Singleton
    McpServerFeatures.SyncToolSpecification searchToolSync(SearchTool searchTool) {
        return McpServerFeatures.SyncToolSpecification.builder()
            .tool(searchTool())
            .callHandler((exchange, req) -> {
                return getCallToolResult(searchTool, req);
            }).build();
    }

    private static McpSchema.CallToolResult getCallToolResult(SearchTool searchTool, McpSchema.CallToolRequest req) {
        Object obj = req.arguments().get(QUERY);
        if (obj != null && obj instanceof String query) {
            SearchToolResults searchToolResults = searchTool.search(query);
            return McpSchema.CallToolResult.builder()
                .structuredContent(searchToolResults)
                .isError(false)
                .build();
        } else {
            return McpSchema.CallToolResult.builder()
                .isError(true)
                .build();
        }
    }

    private McpSchema.Tool searchTool() {
        var builder = McpSchema.Tool.builder()
            .name("search")
            .description("Returns a list of relevant search results, given a user's query.")
            .inputSchema(SEARCH_TOOL_INPUT_SCHEMA);
        jsonSchemaClassPathResourceLoader.jsonSchemaStringForClass(SearchToolResults.class)
            .ifPresent(jsonSchema -> builder.outputSchema(mcpJsonMapper, jsonSchema));
        return builder.build();
    }

    @Requires(beans = SearchTool.class)
    @Singleton
    McpServerFeatures.AsyncToolSpecification searchToolAsync(SearchTool searchTool) {
        return McpServerFeatures.AsyncToolSpecification.builder()
            .tool(searchTool())
            .callHandler((exchange, req) ->
                Mono.just(getCallToolResult(searchTool, req)))
            .build();
    }
//
//    @Requires(beans = SearchTool.class)
//    @Singleton
//    McpStatelessServerFeatures.AsyncToolSpecification searchToolStatelessAsync(SearchTool searchTool) {
//
//    }
//
//    @Requires(beans = SearchTool.class)
//    @Singleton
//    McpStatelessServerFeatures.SyncToolSpecification searchToolStatelessSync(SearchTool searchTool) {
//
//    }
}
