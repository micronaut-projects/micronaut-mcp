package io.micronaut.mcp.docs.tools.fetch

//tag::imports[]
import io.micronaut.context.annotation.Requires
import io.micronaut.mcp.server.tools.fetch.FetchRequest
import io.micronaut.mcp.server.tools.fetch.FetchResponse
import io.micronaut.mcp.server.tools.fetch.FetchTool
import io.modelcontextprotocol.common.McpTransportContext
import jakarta.inject.Singleton

//end::imports[]
@Requires(property = "spec.name", value = "MicronautModulesFetchSpec")
//tag::clazz[]
@Singleton
class MicronautModulesFetch implements FetchTool {

    @Override
    Optional<FetchResponse> fetch(FetchRequest request, McpTransportContext transportContext) {
        Optional.of(FetchResponse.builder()
            .id("micronaut-security")
            .title("Micronaut Security")
            .url("https://micronaut-projects.github.io/micronaut-security/latest/guide")
            .text("Built-in security features. Authentication providers and strategies, Token Propagation.")
            .build())
    }
}
//end::clazz[]
