package io.micronaut.mcp.docs.resources

//tag::imports[]
import io.micronaut.context.annotation.Requires
import io.micronaut.mcp.annotations.ResourceTemplate
import io.modelcontextprotocol.spec.McpError
import io.modelcontextprotocol.spec.McpSchema
import jakarta.inject.Singleton

//end::imports[]
@Requires(property = "spec.name", value = "MyResourcesTemplatesTest")
//tag::clazz[]
@Singleton
class MyResourcesTemplates(private val pgnLoader: PgnLoader) {

    @ResourceTemplate(uriTemplate = "pgn://round/{round}",
        mimeType = PGN_MIME_TYPE,
        name = "2024ChessChampionshipRoundPgn",
        title = "PGN of a round World Chess Championship 2024",
        description = "Given a round, it returns a PGN of the World Chess Championship 2024 between Ding Liren and Gukesh Dommaraju")
    fun pgn(round: Int): String {
        return pgnLoader.loadPgn(round)
            .orElseThrow { McpError(McpSchema.JSONRPCResponse.JSONRPCError(McpSchema.ErrorCodes.RESOURCE_NOT_FOUND, "resource for round not found", null)) }
    }

    companion object {
        private const val PGN_MIME_TYPE = "application/x-chess-pgn"
    }
}
//end::clazz[]
