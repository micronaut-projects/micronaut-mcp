package example.micronaut.structured;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.jsonschema.JsonSchema;

@Introspected
@JsonSchema
public record Evaluation(String fen, String evaluation) {
}
