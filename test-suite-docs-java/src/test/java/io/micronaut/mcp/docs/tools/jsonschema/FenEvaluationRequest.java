package io.micronaut.mcp.docs.tools.jsonschema;

//tag::imports[]
import io.micronaut.jsonschema.JsonSchema;
import io.micronaut.serde.annotation.Serdeable;

//end::imports[]
//tag::clazz[]
/**
 *
 * @param fen A Chess position in Forsyth–Edwards Notation
 */
@JsonSchema
@Serdeable
public record FenEvaluationRequest(String fen) {
}
//end::clazz[]
