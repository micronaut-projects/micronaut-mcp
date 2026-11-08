package io.micronaut.mcp.server.stateless.sync.tools.jsonschema.nulloutput;

import io.micronaut.context.annotation.Requires;
import io.micronaut.mcp.annotations.Tool;
import io.micronaut.mcp.server.stateless.sync.tools.jsonschema.output.FenEvaluationResponse;
import jakarta.inject.Singleton;

@Requires(property = "spec.name", value = "NullStructuredOutputTest")
@Singleton
class NullStructuredOutputTools {
    @Tool(description = "Evaluate a chess position, without an answer.")
    FenEvaluationResponse unknownEvaluation() {
        return null;
    }
}
