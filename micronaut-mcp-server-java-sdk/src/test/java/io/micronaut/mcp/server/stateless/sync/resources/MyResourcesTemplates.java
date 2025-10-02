package io.micronaut.mcp.server.stateless.sync.resources;

import io.micronaut.context.annotation.Requires;
import io.micronaut.mcp.annotations.ResourceTemplate;
import io.micronaut.mcp.server.utils.PgnLoader;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Singleton;

@Requires(property = "spec.name", value = "ResourceTemplatePathVariablesTest")
//tag::clazz[]
@Singleton
class MyResources {
    private static final String PGN_MIME_TYPE = "application/x-chess-pgn";
    private final PgnLoader pgnLoader;

    MyResources(PgnLoader pgnLoader) {
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
