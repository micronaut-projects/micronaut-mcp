package io.micronaut.mcp.docs.tools.jsonschema.output;

//tag::imports[]
import io.micronaut.jsonschema.JsonSchema;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.NotBlank;
import org.jspecify.annotations.NonNull;

//end::imports[]
//tag::clazz[]
@Serdeable
@JsonSchema
public record FenEvaluationResponse(
    @NonNull @NotBlank String fen,
    @NonNull @NotBlank String evaluation
) {
}
//end::clazz[]
