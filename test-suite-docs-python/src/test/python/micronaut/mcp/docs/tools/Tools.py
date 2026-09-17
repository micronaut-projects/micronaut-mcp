# tag::imports[]
from jakarta.inject import Singleton
from micronaut.context.annotation import Requires
from micronaut.mcp.annotations import Tool
from micronaut.mcp.server.context import MicronautMcpTransportContext
# end::imports[]


@Requires(property="spec.name", value="ToolsTest")
# tag::clazz[]
@Singleton
class Tools:
    @Tool(description="Evaluate a chess position using a FEN string.")
    def fen_evaluation(self, fen: str, ctx: MicronautMcpTransportContext) -> str:
        if fen == "r1bqk2r/ppp2ppp/2n5/1BbpP3/3Nn3/8/PPP2PPP/RNBQK2R w KQkq - 1 8":
            return "+0.12"
        return "+0.0"
# end::clazz[]
