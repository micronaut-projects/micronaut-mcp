package example.micronaut.structured;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.annotation.ReflectiveAccess;
import io.micronaut.jsonschema.JsonSchema;

@Introspected
// Jackson databind reads the components of a record by reflection, which a native image only allows for registered types
@ReflectiveAccess
@JsonSchema
public record Evaluation(String fen, String evaluation) {
}
