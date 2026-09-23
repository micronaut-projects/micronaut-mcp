from typing import Annotated

from micronaut.context.annotation import EachProperty, Parameter, Requires
from micronaut.core.naming import Named


@Requires(property="spec.name", value="ResourcesFactoryTest")
@EachProperty("pgn")
class PgnFile(Named):
    """Configuration of a PGN file: ``pgn.<name>.path`` and ``pgn.<name>.round``."""

    path: str | None = None
    round: int | None = None

    def __init__(self, name: Annotated[str, Parameter]):
        self.name = name
