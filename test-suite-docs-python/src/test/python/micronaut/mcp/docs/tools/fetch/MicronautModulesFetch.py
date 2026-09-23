# tag::imports[]
from io.modelcontextprotocol.common import McpTransportContext
from jakarta.inject import Singleton
from java.util import Optional
from micronaut.context.annotation import Requires
from micronaut.mcp.server.tools.fetch import FetchRequest, FetchResponse, FetchTool
# end::imports[]


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
