package io.micronaut.mcp.server.stateless.sync;
/*
//tag::fakepackage[]
package example.micronaut;

//end::fakepackage[]
 */
//tag::imports[]
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;
import io.modelcontextprotocol.server.McpStatelessServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.Map;
//end::imports[]

@Requires(property = "spec.name", value = "ToolsTest")
//tag::clazz[]
@Factory
class ToolsFactory {
    @Singleton
    McpStatelessServerFeatures.SyncToolSpecification getAlertsTools() {
        McpSchema.JsonSchema fenSchema = new McpSchema.JsonSchema("string", null,null, null, null, null);
        McpSchema.JsonSchema inputSchema = new McpSchema.JsonSchema("object", Map.of("fen", fenSchema), List.of("fen"), null, null, null);
        return McpStatelessServerFeatures.SyncToolSpecification.builder()
            .tool(McpSchema.Tool.builder()
                .name("fenEvaluation")
                .description("Evaluate a chess position using a FEN string.")
                .inputSchema(inputSchema)
                .build())
            .callHandler((exchange, arguments) -> {
                //TODO in real life, do this with Stockfish
                return new McpSchema.CallToolResult("+0.27", false);
            })
            .build();
    }
}
//end::clazz[]
