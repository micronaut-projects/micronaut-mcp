package io.micronaut.mcp.docs.resources;

//tag::imports[]
import io.micronaut.context.annotation.Requires;
import io.micronaut.mcp.annotations.ResourceTemplate;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Singleton;

//end::imports[]
@Requires(property = "spec.name", value = "MyResourcesTemplatesTest")
//tag::clazz[]
@Singleton
class MyResourcesTemplates {
    private static final String PGN_MIME_TYPE = "application/x-chess-pgn";
    private final PgnLoader pgnLoader;

    MyResourcesTemplates(PgnLoader pgnLoader) {
        this.pgnLoader = pgnLoader;
    }

    @ResourceTemplate(uriTemplate = "pgn://round/{round}",
        mimeType = PGN_MIME_TYPE,
        name = "2024ChessChampionshipRoundPgn",
        title = "PGN of a round World Chess Championship 2024",
        description = "Given a round, it returns a PGN of the World Chess Championship 2024 between Ding Liren and Gukesh Dommaraju")
    String pgn(Integer round) {
        return pgnLoader.loadPgn(round)
            .orElseThrow(() -> new McpError(new McpSchema.JSONRPCResponse.JSONRPCError(McpSchema.ErrorCodes.RESOURCE_NOT_FOUND, "resource for round not found", null)));
    }
}
//end::clazz[]
