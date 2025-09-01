package io.micronaut.mcp.server.stateless.sync.tools.jsonschema.output;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.jsonschema.JsonSchema;
import jakarta.validation.constraints.NotBlank;

@Introspected
@JsonSchema
public record FenEvaluationResponse(
    @NonNull @NotBlank String fen,
    @NonNull @NotBlank String evaluation
) {
}
