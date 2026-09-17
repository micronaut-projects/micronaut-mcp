# tag::imports[]
from jakarta.inject import Singleton
from java.util import Optional
from micronaut.context.annotation import Requires
from micronaut.mcp.server.tools.fetch import FetchRequest, FetchResponse, FetchTool
# end::imports[]

try:
    from io.modelcontextprotocol.common import McpTransportContext
except ImportError:  # TODO(python): packages under `io.` other than `io.micronaut` cannot be imported at runtime
    from modelcontextprotocol.common import McpTransportContext


@Requires(property="spec.name", value="MicronautModulesFetchTest")
# tag::clazz[]
@Singleton
class MicronautModulesFetch(FetchTool):

    def fetch(self, request: FetchRequest, transport_context: McpTransportContext) -> Optional[FetchResponse]:
        return Optional.of(FetchResponse.builder()
                           .id("micronaut-security")
                           .title("Micronaut Security")
                           .url("https://micronaut-projects.github.io/micronaut-security/latest/guide")
                           .text("Built-in security features. Authentication providers and strategies, Token Propagation.")
                           .build())
# end::clazz[]
