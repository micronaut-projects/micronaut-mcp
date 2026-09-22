# tag::imports[]
from io.modelcontextprotocol.server import McpStatelessServerFeatures
from io.modelcontextprotocol.spec import McpSchema
from jakarta.inject import Singleton
from java.util import List, Map
from micronaut.context.annotation import Factory, Requires
# end::imports[]


@Requires(property="spec.name", value="ToolsFactoryTest")
# tag::clazz[]
@Factory
class ToolsFactory:
    @Singleton
    def fen_evaluation_tool(self) -> McpStatelessServerFeatures.SyncToolSpecification:
        return (McpStatelessServerFeatures.SyncToolSpecification.builder()
                .tool(self.tool())
                .callHandler(lambda ctx, req: self.call_tool(req))
                .build())

    def tool(self) -> McpSchema.Tool:
        return (McpSchema.Tool.builder()
                .name("fenEvaluation")
                .description("Evaluate a chess position using a FEN string.")
                .inputSchema(self.input_schema())
                .build())

    def input_schema(self) -> McpSchema.JsonSchema:
        fen_schema = McpSchema.JsonSchema("string", None, None, None, None, None)
        return McpSchema.JsonSchema("object", Map.of("fen", fen_schema), List.of("fen"), None, None, None)

    def call_tool(self, req: McpSchema.CallToolRequest) -> McpSchema.CallToolResult:
        content = self.evaluation(str(req.arguments().get("fen")))
        return McpSchema.CallToolResult.builder().addTextContent(content).isError(False).build()

    def evaluation(self, fen: str) -> str:
        if fen == "r1bqk2r/ppp2ppp/2n5/1BbpP3/3Nn3/8/PPP2PPP/RNBQK2R w KQkq - 1 8":
            return "+0.12"
        return "+0.0"
# end::clazz[]
