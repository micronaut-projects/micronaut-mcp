package io.micronaut.mcp.docs.tools
/*
//tag::fakepackage[]
package example.micronaut

//end::fakepackage[]
 */
//tag::imports[]
import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Requires
import io.modelcontextprotocol.server.McpStatelessServerFeatures
import io.modelcontextprotocol.common.McpTransportContext
import io.modelcontextprotocol.spec.McpSchema
import jakarta.inject.Singleton

import java.util.function.BiFunction
//end::imports[]

@Requires(property = "spec.name", value = "ToolsFactorySpec")
//tag::clazz[]
@Factory
class ToolsFactory {
    @Singleton
    McpStatelessServerFeatures.SyncToolSpecification fenEvaluationTool() {
        McpStatelessServerFeatures.SyncToolSpecification.builder()
            .tool(tool())
            .callHandler(callHandler())
            .build()
    }

    private McpSchema.Tool tool() {
        McpSchema.Tool.builder()
            .name("fenEvaluation")
            .description("Evaluate a chess position using a FEN string.")
            .inputSchema(inputSchema())
            .build()
    }

    private McpSchema.JsonSchema inputSchema() {
        McpSchema.JsonSchema fenSchema = new McpSchema.JsonSchema("string", null, null, null, null, null)
        new McpSchema.JsonSchema("object", [fen: fenSchema], ["fen"], null, null, null)
    }

    private BiFunction<McpTransportContext, McpSchema.CallToolRequest, McpSchema.CallToolResult> callHandler() {
        return { McpTransportContext ctx, McpSchema.CallToolRequest req ->
            String content = evaluation(req.arguments().get("fen").toString())
            McpSchema.CallToolResult.builder().addTextContent(content).isError(false).build()
        } as BiFunction<McpTransportContext, McpSchema.CallToolRequest, McpSchema.CallToolResult>
    }

    private String evaluation(String fen) {
        if (fen == "r1bqk2r/ppp2ppp/2n5/1BbpP3/3Nn3/8/PPP2PPP/RNBQK2R w KQkq - 1 8") {
            return "+0.12"
        }
        return "+0.0"
    }
}
//end::clazz[]
