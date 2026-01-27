package io.micronaut.mcp.server.stateless.sync.tools.toolargparameters;
/*
//tag::fakepackage[]
package example.micronaut;

//end::fakepackage[]
 */
//tag::imports[]
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.jsonschema.JsonSchema;
import io.micronaut.mcp.annotations.Tool;
import io.micronaut.mcp.annotations.ToolArg;
import io.modelcontextprotocol.common.McpTransportContext;
import jakarta.inject.Singleton;

import java.util.List;

//end::imports[]

@Requires(property = "spec.name", value = "StatelessSyncToolsArgParametersTest")
//tag::clazz[]
@Singleton
class Tools {

    // Test Req Shema
    @JsonSchema
    @Introspected
    record TestReq(String a, String b) {

    }

    @Tool(name = "fenEvaluation", description = "Evaluate a chess position using a FEN string.")
    String forsythEdwardsNotationEvaluation(@ToolArg(name = "fen") String forsythEdwardsNotation,
                                            @ToolArg(name = "fenList") List<String> forsythEdwardsNotations,
                                            @ToolArg(name = "tetBol") boolean tetBol,
                                            @ToolArg(name = "tetWrBol") Boolean tetWrBol,
                                            @ToolArg(name = "tetLong") long tetLong,
                                            @ToolArg(name = "tetWrLong") Long tetWrLong,
                                            @ToolArg(name = "testReq")  TestReq testReq,
                                            McpTransportContext ctx) {
        if (forsythEdwardsNotation.equals("r1bqk2r/ppp2ppp/2n5/1BbpP3/3Nn3/8/PPP2PPP/RNBQK2R w KQkq - 1 8")) {
            return "+0.12";
        }
        return "+0.0";
    }
}
//end::clazz[]
