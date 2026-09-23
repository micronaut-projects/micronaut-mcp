# tag::imports[]
from dataclasses import dataclass
from typing import Annotated

from jakarta.validation.constraints import NotBlank
from micronaut.jsonschema import JsonSchema
from micronaut.serde.annotation import Serdeable
from org.jspecify.annotations import NonNull
# end::imports[]


# tag::clazz[]
@Serdeable
@JsonSchema
@dataclass
class FenEvaluationResponse:
    fen: Annotated[str, NonNull, NotBlank]
    evaluation: Annotated[str, NonNull, NotBlank]
# end::clazz[]
