package example.micronaut.mcp;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.jsonschema.JsonSchema;

/**
 *
 * @param state Two-letter US state code (e.g. CA, NY)
 */
@Introspected
@JsonSchema
public record GetAlertInput(String state) {
}
