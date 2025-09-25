package io.micronaut.mcp.server.tools.fetch;

import io.micronaut.context.annotation.Requires;
import io.micronaut.mcp.server.context.MicronautMcpTransportContext;
import jakarta.inject.Singleton;

import java.util.Optional;

@Requires(property = "spec.tool.name", value = "MicronautModulesFetch")
//tag::clazz[]
@Singleton
class MicronautModulesFetch implements FetchTool {

    @Override
    public Optional<FetchResponse> fetch(FetchRequest request, MicronautMcpTransportContext transportContext) {
        return Optional.of(FetchResponse.builder()
            .id("micronaut-security")
            .title("Micronaut Security")
            .url("https://micronaut-projects.github.io/micronaut-security/latest/guide")
            .text("Built-in security features. Authentication providers and strategies, Token Propagation.")
            .build());
    }
}
//end::clazz[]
