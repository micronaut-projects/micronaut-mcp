# tag::imports[]
from jakarta.inject import Singleton
from micronaut.context.annotation import Requires
from micronaut.mcp.annotations import Tool
from micronaut.mcp.server.context import MicronautMcpTransportContext

from .FenEvaluationRequest import FenEvaluationRequest
# end::imports[]


@Requires(property="spec.name", value="JsonSchemaToolsTest")
# tag::clazz[]
@Singleton
class Tools:
    @Tool(description="Evaluate a chess position using a FEN string.")
    def fen_evaluation(self, req: FenEvaluationRequest, ctx: MicronautMcpTransportContext) -> str:
        if req.fen == "r1bqk2r/ppp2ppp/2n5/1BbpP3/3Nn3/8/PPP2PPP/RNBQK2R w KQkq - 1 8":
            return "+0.12"
        return "+0.0"
# end::clazz[]
