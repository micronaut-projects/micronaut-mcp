package io.micronaut.mcp.chatgpt;

import io.micronaut.jsonschema.JsonSchema;
import io.micronaut.serde.annotation.Serdeable;

/**
 *
 */
@Serdeable
@JsonSchema
public record SearchToolInput(String query) {
}
