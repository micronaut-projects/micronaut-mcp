package io.micronaut.mcp.server.stateless.sync.tools.jsonschema.output;

import io.micronaut.core.annotation.Introspected;
import org.jspecify.annotations.NonNull;
import io.micronaut.jsonschema.JsonSchema;
import jakarta.validation.constraints.NotBlank;

@Introspected
@JsonSchema
public record FenEvaluationResponse(
    @NonNull @NotBlank String fen,
    @NonNull @NotBlank String evaluation
) {
}
