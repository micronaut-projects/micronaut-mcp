package io.micronaut.mcp.docs.security

import io.micronaut.context.annotation.Requires
//tag::imports[]
import io.micronaut.mcp.annotations.Tool
import io.micronaut.mcp.server.context.MicronautMcpTransportContext
import jakarta.inject.Singleton
//end::imports[]

@Requires(property = "spec.name", value = "WhoAmIToolsSpec")
//tag::clazz[]
@Singleton
class WhoAmITools {
    @Tool(description = "Says who the authenticated user is")
    String whoami(MicronautMcpTransportContext context) {
        context.principal()?.name ?: "anonymous"
    }
}
//end::clazz[]
