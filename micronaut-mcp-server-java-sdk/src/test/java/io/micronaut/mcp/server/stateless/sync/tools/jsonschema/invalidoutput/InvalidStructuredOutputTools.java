package io.micronaut.mcp.server.stateless.sync.tools.jsonschema.invalidoutput;

import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.jsonschema.JsonSchema;
import io.micronaut.mcp.annotations.Tool;
import io.micronaut.mcp.server.stateless.sync.tools.jsonschema.output.FenEvaluationResponse;
import jakarta.inject.Singleton;

@Requires(property = "spec.name", value = "InvalidStructuredOutputTest")
@Singleton
class InvalidStructuredOutputTools {
    @Tool(description = "Evaluate a chess position, with blank values.")
    FenEvaluationResponse blankEvaluation() {
        return new FenEvaluationResponse("", "");
    }

    @Tool(description = "Evaluate a chess position, with a value that cannot be serialized.")
    UnserializableEvaluation unserializableEvaluation() {
        return new UnserializableEvaluation(new Unserializable());
    }

    @JsonSchema
    @Introspected
    record UnserializableEvaluation(Unserializable evaluation) {
    }

    /**
     * Has no serializable introspection.
     */
    static final class Unserializable {
    }
}
