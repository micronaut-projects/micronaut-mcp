package io.micronaut.mcp.docs.tools.search;

//tag::imports[]
import io.micronaut.context.annotation.Requires;
import io.micronaut.mcp.server.tools.search.SearchRequest;
import io.micronaut.mcp.server.tools.search.SearchResponse;
import io.micronaut.mcp.server.tools.search.SearchResult;
import io.micronaut.mcp.server.tools.search.SearchTool;
import io.modelcontextprotocol.common.McpTransportContext;
import jakarta.inject.Singleton;

import java.util.List;

//end::imports[]
@Requires(property = "spec.name", value = "MicronautModulesSearchTest")
//tag::clazz[]
@Singleton
class MicronautModulesSearch implements SearchTool {

    @Override
    public SearchResponse search(SearchRequest request, McpTransportContext transportContext) {
        return new SearchResponse(List.of(SearchResult.builder()
            .id("micronaut-security")
            .title("Micronaut Security")
            .url("https://micronaut-projects.github.io/micronaut-security/latest/guide")
            .build()));
    }
}
//end::clazz[]
