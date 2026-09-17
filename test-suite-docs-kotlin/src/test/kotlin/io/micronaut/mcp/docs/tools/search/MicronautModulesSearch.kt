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
@Requires(property = "spec.name", value = "MicronautModulesSearchTest")
//tag::clazz[]
@Singleton
class MicronautModulesSearch : SearchTool {

    override fun search(request: SearchRequest, transportContext: McpTransportContext?): SearchResponse {
        return SearchResponse(listOf(SearchResult.builder()
            .id("micronaut-security")
            .title("Micronaut Security")
            .url("https://micronaut-projects.github.io/micronaut-security/latest/guide")
            .build()))
    }
}
//end::clazz[]
