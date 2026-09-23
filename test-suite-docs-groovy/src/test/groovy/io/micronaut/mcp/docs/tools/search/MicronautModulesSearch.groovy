package io.micronaut.mcp.docs.tools.search

//tag::imports[]
import io.micronaut.context.annotation.Requires
import io.micronaut.mcp.server.tools.search.SearchRequest
import io.micronaut.mcp.server.tools.search.SearchResponse
import io.micronaut.mcp.server.tools.search.SearchResult
import io.micronaut.mcp.server.tools.search.SearchTool
import io.modelcontextprotocol.common.McpTransportContext
import jakarta.inject.Singleton

//end::imports[]
@Requires(property = "spec.name", value = "MicronautModulesSearchSpec")
//tag::clazz[]
@Singleton
class MicronautModulesSearch implements SearchTool {

    @Override
    SearchResponse search(SearchRequest request, McpTransportContext transportContext) {
        new SearchResponse([SearchResult.builder()
            .id("micronaut-security")
            .title("Micronaut Security")
            .url("https://micronaut-projects.github.io/micronaut-security/latest/guide")
            .build()])
    }
}
//end::clazz[]
