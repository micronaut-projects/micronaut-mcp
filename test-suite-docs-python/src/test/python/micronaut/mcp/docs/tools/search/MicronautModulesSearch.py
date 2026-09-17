# tag::imports[]
from jakarta.inject import Singleton
from java.util import List
from micronaut.context.annotation import Requires
from micronaut.mcp.server.tools.search import SearchRequest, SearchResponse, SearchResult, SearchTool
# end::imports[]

try:
    from io.modelcontextprotocol.common import McpTransportContext
except ImportError:  # TODO(python): packages under `io.` other than `io.micronaut` cannot be imported at runtime
    from modelcontextprotocol.common import McpTransportContext


@Requires(property="spec.name", value="MicronautModulesSearchTest")
# tag::clazz[]
@Singleton
class MicronautModulesSearch(SearchTool):

    def search(self, request: SearchRequest, transport_context: McpTransportContext) -> SearchResponse:
        return SearchResponse(List.of(SearchResult.builder()
                                      .id("micronaut-security")
                                      .title("Micronaut Security")
                                      .url("https://micronaut-projects.github.io/micronaut-security/latest/guide")
                                      .build()))
# end::clazz[]
