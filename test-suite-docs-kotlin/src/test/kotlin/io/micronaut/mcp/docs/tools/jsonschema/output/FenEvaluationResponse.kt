package io.micronaut.mcp.docs.tools.jsonschema.output

//tag::imports[]
import io.micronaut.jsonschema.JsonSchema
import io.micronaut.serde.annotation.Serdeable
import jakarta.validation.constraints.NotBlank

//end::imports[]
//tag::clazz[]
@Serdeable
@JsonSchema
data class FenEvaluationResponse(
    @field:NotBlank val fen: String,
    @field:NotBlank val evaluation: String
)
//end::clazz[]
