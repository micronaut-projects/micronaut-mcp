package io.micronaut.mcp.server.stateless.sync.tools;


import io.micronaut.context.annotation.Requires;
import io.micronaut.mcp.annotations.Tool;
import jakarta.inject.Singleton;

@Requires(property = "spec.name", value = "ToolAnnotationsTest")
//tag::clazz[]
@Singleton
class HelloWorldTool {
    @Tool(title = "Hello World",
        annotations = @Tool.ToolAnnotations(readOnlyHint = true,
            title = "Hello World",
            destructiveHint = false,
            idempotentHint = true,
            openWorldHint = false,
            returnDirect = true))
    String helloWorld() {
        return "Hello, World!";
    }
}
//end::clazz[]
