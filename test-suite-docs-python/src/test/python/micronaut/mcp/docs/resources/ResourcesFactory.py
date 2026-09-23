# tag::imports[]
from io.modelcontextprotocol.server import McpStatelessServerFeatures
from io.modelcontextprotocol.spec import McpSchema
from jakarta.inject import Singleton
from java.util import ArrayList
from micronaut.context.annotation import Context, EachBean, Factory, Requires
from micronaut.context.exceptions import ConfigurationException
from micronaut.core.io import ResourceLoader

from .PgnFile import PgnFile
from .PgnLoader import PgnLoader
# end::imports[]


# tag::clazz[]
PGN_MIME_TYPE = "application/x-chess-pgn"


def round_of(uri: str) -> int:
    last_slash = uri.rfind("/")
    return int(uri[last_slash + 1:])


def read_resource_result(uri: str, pgn_loader: PgnLoader) -> McpSchema.ReadResourceResult:
    round = round_of(uri)
    contents = ArrayList()
    text = pgn_loader.load_pgn(round)
    if text is not None:
        contents.add(McpSchema.TextResourceContents(uri, PGN_MIME_TYPE, text))
    return McpSchema.ReadResourceResult(contents)


# end::clazz[]
@Requires(property="spec.name", value="ResourcesFactoryTest")
# tag::clazz[]
@Context
@Factory
class ResourcesFactory:

    def __init__(self, resource_loader: ResourceLoader, pgn_loader: PgnLoader):
        self.resource_loader = resource_loader
        self.pgn_loader = pgn_loader

    @EachBean(PgnFile)
    @Singleton
    def create_pgn_sync_resource_specification(self, pgn_file: PgnFile) -> McpStatelessServerFeatures.SyncResourceSpecification:
        resource = self.get_resource(pgn_file)
        return McpStatelessServerFeatures.SyncResourceSpecification(
            resource,
            lambda ctx, read_resource_request: read_resource_result(read_resource_request.uri(), self.pgn_loader))

    def get_resource(self, pgn_file: PgnFile) -> McpSchema.Resource:
        size = self.size(pgn_file.path)
        if size is None:
            raise ConfigurationException("unable find resource for path " + pgn_file.path)
        round = pgn_file.round
        uri = f"pgn://round/{round}"
        name = f"round{round}PgnFideWCC2024"
        title = f"PGN of the Round {round} game of the World Chess Championship"
        description = f"{title} between Ding Liren and Gukesh Dommaraju"
        return McpSchema.Resource(uri, name, title, description, PGN_MIME_TYPE, size, None, None)

    def size(self, path: str) -> int | None:
        input_stream_optional = self.resource_loader.getResourceAsStream(path)
        if input_stream_optional.isPresent():
            input_stream = input_stream_optional.get()
            try:
                return len(input_stream.readAllBytes())
            finally:
                input_stream.close()
        return None
# end::clazz[]
