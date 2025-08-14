package io.micronaut.mcp.server.stateless.sync;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.jsonschema.JsonSchema;

@JsonSchema
@Introspected
public record EvaluationResult(String fen, String evaluation) {
}
