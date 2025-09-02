package io.micronaut.mcp.server.stateless.sync.tools.jsonschema.output;
/*
//tag::fakepackage[]
package example.micronaut;

//end::fakepackage[]
 */
//tag::imports[]
import io.micronaut.context.annotation.Requires;
import io.micronaut.mcp.annotations.Tool;
import io.micronaut.mcp.server.stateless.sync.tools.jsonschema.FenEvaluationRequest;
import jakarta.inject.Singleton;

//end::imports[]

@Requires(property = "spec.name", value = "StatelessSyncToolAnnotationInputOutputJsonSchemaTest")
//tag::clazz[]
@Singleton
class Tools {
    @Tool(description = "Evaluate a chess position using a FEN string.")
    FenEvaluationResponse fenEvaluation(FenEvaluationRequest req) {
        String fen = req.fen();
        if (fen.equals("r1bqk2r/ppp2ppp/2n5/1BbpP3/3Nn3/8/PPP2PPP/RNBQK2R w KQkq - 1 8")) {
            return new FenEvaluationResponse(fen, "+0.12");
        }
        return new FenEvaluationResponse(fen, "+0.0");
    }
}
//end::clazz[]
