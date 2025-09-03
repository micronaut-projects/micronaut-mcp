package io.micronaut.mcp.server.stateless.sync.tools.jsonschema;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.jsonschema.JsonSchema;

/**
 *
 * @param fen A Chess position in Forsyth–Edwards Notation
 */
@JsonSchema
@Introspected
public record FenEvaluationRequest(String fen) {
}
