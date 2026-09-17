# tag::imports[]
from jakarta.inject import Singleton
from micronaut.context.annotation import Factory, Requires

from .PgnLoader import PgnLoader
from .ResourcesFactory import PGN_MIME_TYPE, read_resource_result
# end::imports[]

try:
    from io.modelcontextprotocol.server import McpStatelessServerFeatures
    from io.modelcontextprotocol.spec import McpSchema
except ImportError:  # TODO(python): packages under `io.` other than `io.micronaut` cannot be imported at runtime
    from modelcontextprotocol.server import McpStatelessServerFeatures
    from modelcontextprotocol.spec import McpSchema


@Requires(property="spec.name", value="ResourcesFactoryTest")
# tag::clazz[]
@Factory
class ResourcesTemplatesFactory:

    def __init__(self, pgn_loader: PgnLoader):
        self.pgn_loader = pgn_loader

    @Singleton
    def pgn_resource_template_specification(self) -> McpStatelessServerFeatures.SyncResourceTemplateSpecification:
        resource_template = self.create_pgn_resource_template()
        return McpStatelessServerFeatures.SyncResourceTemplateSpecification(
            resource_template,
            lambda ctx, read_resource_request: read_resource_result(read_resource_request.uri(), self.pgn_loader))

    def create_pgn_resource_template(self) -> McpSchema.ResourceTemplate:
        uri_template = "pgn://round/{round}"
        name = "2024ChessChampionshipRoundPgn"
        title = "PGN of a round World Chess Championship 2024"
        description = "Given a round, it returns a PGN of the World Chess Championship 2024 between Ding Liren and Gukesh Dommaraju"
        return McpSchema.ResourceTemplate(uri_template, name, title, description, PGN_MIME_TYPE, None, None)
# end::clazz[]
