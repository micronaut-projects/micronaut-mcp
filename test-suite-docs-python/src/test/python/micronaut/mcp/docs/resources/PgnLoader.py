import logging

from jakarta.inject import Singleton
from java.lang import String
from java.nio.charset import StandardCharsets
from micronaut.core.io import ResourceLoader

LOG = logging.getLogger(__name__)


@Singleton
class PgnLoader:
    """Loads the PGN of a round of the World Chess Championship 2024 from the classpath."""

    def __init__(self, resource_loader: ResourceLoader):
        self.resource_loader = resource_loader

    def load_pgn(self, round: int) -> str | None:
        pgn_input_stream = self.resource_loader.getResourceAsStream(f"classpath:fidewwc2024/round_{round}.pgn")
        if pgn_input_stream.isEmpty():
            return None
        input_stream = pgn_input_stream.get()
        try:
            return str(String(input_stream.readAllBytes(), StandardCharsets.UTF_8))
        except Exception as e:
            LOG.error("%s", e, exc_info=e)
            return None
        finally:
            input_stream.close()
