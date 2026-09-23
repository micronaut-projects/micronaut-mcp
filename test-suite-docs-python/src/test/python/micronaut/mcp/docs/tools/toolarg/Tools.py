# tag::imports[]
from typing import Annotated

from io.modelcontextprotocol.common import McpTransportContext
from jakarta.inject import Singleton
from micronaut.context.annotation import Requires
from micronaut.mcp.annotations import Tool, ToolArg
# end::imports[]


@Requires(property="spec.name", value="ToolArgToolsTest")
# tag::clazz[]
@Singleton
class Tools:
    @Tool(name="fenEvaluation", description="Evaluate a chess position using a FEN string.")
    def forsyth_edwards_notation_evaluation(self,
                                            forsyth_edwards_notation: Annotated[str, ToolArg(name="fen")],
                                            ctx: McpTransportContext) -> str:
        if forsyth_edwards_notation == "r1bqk2r/ppp2ppp/2n5/1BbpP3/3Nn3/8/PPP2PPP/RNBQK2R w KQkq - 1 8":
            return "+0.12"
        return "+0.0"
# end::clazz[]
