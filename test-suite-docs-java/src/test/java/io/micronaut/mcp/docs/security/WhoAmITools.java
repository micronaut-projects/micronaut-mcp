package io.micronaut.mcp.docs.security;

import io.micronaut.context.annotation.Requires;
//tag::imports[]
import io.micronaut.mcp.annotations.Tool;
import io.micronaut.mcp.server.context.MicronautMcpTransportContext;
import jakarta.inject.Singleton;

import java.security.Principal;
//end::imports[]

@Requires(property = "spec.name", value = "WhoAmIToolsTest")
//tag::clazz[]
@Singleton
class WhoAmITools {
    @Tool(description = "Says who the authenticated user is")
    String whoami(MicronautMcpTransportContext context) {
        Principal principal = context.principal();
        return principal != null ? principal.getName() : "anonymous";
    }
}
//end::clazz[]
