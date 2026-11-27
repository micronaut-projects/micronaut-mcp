package example.micronaut.structured;

import io.micronaut.context.annotation.Requires;
import io.micronaut.mcp.annotations.Tool;
import jakarta.inject.Singleton;

@Requires(property = "spec.name", value = "StructuredToolsHttpTest")
@Singleton
class StructuredTools {
    @Tool(description = "Evaluates the starting position")
    Evaluation startEvaluation() {
        return new Evaluation("start", "+0.2");
    }

    @Tool(description = "Evaluates nothing")
    Evaluation missingEvaluation() {
        return null;
    }
}
