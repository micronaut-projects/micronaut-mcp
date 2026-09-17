from typing import Annotated

from micronaut.context.annotation import EachProperty, Parameter, Requires


# TODO(python): the Java class implements `io.micronaut.core.naming.Named`; a Python class with a `name` attribute cannot
# implement it (the generated property accessor and the bridged interface method are both named `getName()`)
@Requires(property="spec.name", value="ResourcesFactoryTest")
@EachProperty("pgn")
class PgnFile:
    """Configuration of a PGN file: ``pgn.<name>.path`` and ``pgn.<name>.round``."""

    path: str | None = None
    round: int | None = None

    def __init__(self, name: Annotated[str, Parameter]):
        self.name = name
