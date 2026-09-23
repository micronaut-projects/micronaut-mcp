# tag::imports[]
from io.modelcontextprotocol.spec import McpError, McpSchema
from jakarta.inject import Singleton
from micronaut.context.annotation import Requires
from micronaut.mcp.annotations import ResourceTemplate

from .PgnLoader import PgnLoader
# end::imports[]


# tag::clazz[]
PGN_MIME_TYPE = "application/x-chess-pgn"


# end::clazz[]
@Requires(property="spec.name", value="MyResourcesTemplatesTest")
# tag::clazz[]
@Singleton
class MyResourcesTemplates:

    def __init__(self, pgn_loader: PgnLoader):
        self.pgn_loader = pgn_loader

    @ResourceTemplate(uriTemplate="pgn://round/{round}",
                      mimeType=PGN_MIME_TYPE,
                      name="2024ChessChampionshipRoundPgn",
                      title="PGN of a round World Chess Championship 2024",
                      description="Given a round, it returns a PGN of the World Chess Championship 2024 between Ding Liren and Gukesh Dommaraju")
    def pgn(self, round: int) -> str:
        pgn = self.pgn_loader.load_pgn(round)
        if pgn is None:
            raise McpError(McpSchema.JSONRPCResponse.JSONRPCError(McpSchema.ErrorCodes.RESOURCE_NOT_FOUND, "resource for round not found", None))
        return pgn
# end::clazz[]
