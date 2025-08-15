package io.micronaut.mcp.server.stateless.sync.tools;
/*
//tag::fakepackage[]
package example.micronaut;

//end::fakepackage[]
 */
//tag::imports[]
import io.micronaut.context.annotation.Requires;
import io.micronaut.mcp.annotations.Tool;
import io.micronaut.mcp.server.stateless.sync.EvaluationResult;
import jakarta.inject.Singleton;
//end::imports[]

@Requires(property = "spec.name", value = "StatelessSyncToolsAnnotationsObjectReturnTest")
//tag::clazz[]
@Singleton
class ToolsObjectReturn {
    @Tool(description = "Evaluate a chess position using a FEN string.")
    EvaluationResult fenEvaluation(String fen) {
        return new EvaluationResult(fen, "+0.27");
    }
}
//end::clazz[]
