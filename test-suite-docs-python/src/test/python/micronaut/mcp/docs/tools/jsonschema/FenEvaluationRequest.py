# tag::imports[]
from dataclasses import dataclass

from micronaut.jsonschema import JsonSchema
from micronaut.serde.annotation import Serdeable
# end::imports[]


# tag::clazz[]
@JsonSchema
@Serdeable
@dataclass
class FenEvaluationRequest:
    fen: str
    """A Chess position in Forsyth–Edwards Notation"""
# end::clazz[]
