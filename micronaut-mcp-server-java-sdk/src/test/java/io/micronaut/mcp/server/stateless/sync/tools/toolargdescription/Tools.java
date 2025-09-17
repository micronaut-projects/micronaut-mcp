package io.micronaut.mcp.server.stateless.sync.tools.toolargdescription;

import io.micronaut.context.annotation.Requires;
import io.micronaut.mcp.annotations.Tool;
import io.micronaut.mcp.annotations.ToolArg;
import jakarta.inject.Singleton;

@Requires(property = "spec.name", value = "StatelessSyncToolArgDescriptionTest")
@Singleton
class Tools {
    @Tool(name = "fenEvaluation", description = "Evaluate a chess position using a FEN string.")
    String forsythEdwardsNotationEvaluation(@ToolArg(name = "fen", description = "A Chess position in Forsyth–Edwards Notation") String forsythEdwardsNotation) {
        if (forsythEdwardsNotation.equals("r1bqk2r/ppp2ppp/2n5/1BbpP3/3Nn3/8/PPP2PPP/RNBQK2R w KQkq - 1 8")) {
            return "+0.12";
        }
        return "+0.0";
    }
}