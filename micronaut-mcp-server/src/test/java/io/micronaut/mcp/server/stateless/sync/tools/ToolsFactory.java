package io.micronaut.mcp.server.stateless.sync.tools;
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
            .callHandler((exchange, req) -> {
                String evaluation = "+0.0";
                if (req.arguments().get("fen").equals("r1bqk2r/ppp2ppp/2n5/1BbpP3/3Nn3/8/PPP2PPP/RNBQK2R w KQkq - 1 8")) {
                    evaluation = "+0.27";
                }
                return new McpSchema.CallToolResult(evaluation, false);
            })
            .build();
    }
}
//end::clazz[]
