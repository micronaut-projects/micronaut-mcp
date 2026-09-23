package io.micronaut.mcp.docs.tools.jsonschema

//tag::imports[]
import io.micronaut.jsonschema.JsonSchema
import io.micronaut.serde.annotation.Serdeable

//end::imports[]
//tag::clazz[]
@JsonSchema
@Serdeable
data class FenEvaluationRequest(
    /** A Chess position in Forsyth–Edwards Notation */
    val fen: String
)
//end::clazz[]
