package io.micronaut.mcp.server.tools.search;

import io.micronaut.context.annotation.Requires;
import io.modelcontextprotocol.common.McpTransportContext;
import jakarta.inject.Singleton;

import java.util.List;

@Requires(property = "spec.tool.name", value = "MicronautModulesSearch")
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
