package example.micronaut.mcp;

import io.micronaut.jsonschema.JsonSchema;
import io.micronaut.serde.annotation.Serdeable;

/**
 *
 * @param state Two-letter US state code (e.g. CA, NY)
 */
@Serdeable
@JsonSchema
public record GetAlertInput(String state) {
}
