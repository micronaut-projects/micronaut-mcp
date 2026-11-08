package io.micronaut.mcp.client.javasdk.multi;

import io.micronaut.context.annotation.Requires;
import io.micronaut.mcp.annotations.Tool;
import io.micronaut.mcp.server.context.McpRequestContext;
import io.micronaut.runtime.Micronaut;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.Map;

/**
 * An MCP server over STDIO, started by {@link JavaSdkClientsTest} as a process.
 */
public final class StdioServer {
    private StdioServer() {
    }

    public static void main(String[] args) {
        Micronaut.build(args)
            .banner(false)
            .properties(Map.of(
                "micronaut.mcp.server.transport", "STDIO",
                "micronaut.mcp.server.info.name", "stdio-server",
                "micronaut.mcp.server.info.version", "1.0.0",
                "micronaut.server.port", -1,
                "spec.name", "JavaSdkStdioServer"))
            .start();
    }

    @Requires(property = "spec.name", value = "JavaSdkStdioServer")
    @Singleton
    static class StdioTools {
        @Tool(description = "Says hello")
        String hello(String name) {
            return "Hello " + name;
        }

        @Tool(description = "Asks the client's model for a haiku")
        String haiku(McpRequestContext context) {
            McpSchema.CreateMessageResult result = context.sample(McpSchema.CreateMessageRequest.builder()
                .messages(List.of(new McpSchema.SamplingMessage(McpSchema.Role.USER, new McpSchema.TextContent("Write a haiku"))))
                .maxTokens(100)
                .build());
            return "The model wrote: " + ((McpSchema.TextContent) result.content()).text();
        }
    }
}
