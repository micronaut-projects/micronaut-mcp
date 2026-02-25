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
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
//end::imports[]

@Requires(property = "spec.name", value = "ToolsTest")
//tag::clazz[]
@Factory
class ToolsFactory {
    @Singleton
    McpStatelessServerFeatures.SyncToolSpecification getAlertsTools() {
        return McpStatelessServerFeatures.SyncToolSpecification.builder()
            .tool(tool())
            .callHandler(callHandler())
            .build();
    }

    private McpSchema.Tool tool() {
        return McpSchema.Tool.builder()
                .name("fenEvaluation")
                .description("Evaluate a chess position using a FEN string.")
                .inputSchema(inputSchema())
                .build();
    }

    private McpSchema.JsonSchema inputSchema() {
        McpSchema.JsonSchema fenSchema = new McpSchema.JsonSchema("string", null,null, null, null, null);
        return new McpSchema.JsonSchema("object", Map.of("fen", fenSchema), List.of("fen"), null, null, null);
    }

    private BiFunction<McpTransportContext, McpSchema.CallToolRequest, McpSchema.CallToolResult> callHandler() {
        return (exchange, req) -> {
            String content = evaluation(req.arguments().get("fen").toString());
            return McpSchema.CallToolResult.builder().addTextContent(content).isError(false).build();
        };
    }

    private String evaluation(String fen) {
        if (fen.equals("r1bqk2r/ppp2ppp/2n5/1BbpP3/3Nn3/8/PPP2PPP/RNBQK2R w KQkq - 1 8")) {
            return "+0.12";
        }
        return "+0.0";
    }
}
//end::clazz[]
