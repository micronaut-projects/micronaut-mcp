package io.micronaut.mcp.docs.tools.jsonschema.output

//tag::imports[]
import io.micronaut.jsonschema.JsonSchema
import io.micronaut.serde.annotation.Serdeable
import jakarta.validation.constraints.NotBlank
import org.jspecify.annotations.NonNull

//end::imports[]
//tag::clazz[]
@Serdeable
@JsonSchema
class FenEvaluationResponse {
    @NonNull
    @NotBlank
    final String fen

    @NonNull
    @NotBlank
    final String evaluation

    FenEvaluationResponse(String fen, String evaluation) {
        this.fen = fen
        this.evaluation = evaluation
    }
}
//end::clazz[]
