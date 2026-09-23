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

@Requires(property = "spec.name", value = "ToolsFactoryTest")
//tag::clazz[]
@Factory
class ToolsFactory {
    @Singleton
    fun fenEvaluationTool(): McpStatelessServerFeatures.SyncToolSpecification {
        return McpStatelessServerFeatures.SyncToolSpecification.builder()
            .tool(tool())
            .callHandler(callHandler())
            .build()
    }

    private fun tool(): McpSchema.Tool {
        return McpSchema.Tool.builder()
            .name("fenEvaluation")
            .description("Evaluate a chess position using a FEN string.")
            .inputSchema(inputSchema())
            .build()
    }

    private fun inputSchema(): McpSchema.JsonSchema {
        val fenSchema = McpSchema.JsonSchema("string", null, null, null, null, null)
        return McpSchema.JsonSchema("object", mapOf("fen" to fenSchema), listOf("fen"), null, null, null)
    }

    private fun callHandler(): BiFunction<McpTransportContext, McpSchema.CallToolRequest, McpSchema.CallToolResult> {
        return BiFunction { _, req ->
            val content = evaluation(req.arguments()["fen"].toString())
            McpSchema.CallToolResult.builder().addTextContent(content).isError(false).build()
        }
    }

    private fun evaluation(fen: String): String {
        if (fen == "r1bqk2r/ppp2ppp/2n5/1BbpP3/3Nn3/8/PPP2PPP/RNBQK2R w KQkq - 1 8") {
            return "+0.12"
        }
        return "+0.0"
    }
}
//end::clazz[]
