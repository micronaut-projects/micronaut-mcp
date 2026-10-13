package io.micronaut.mcp.client.langchain4j.multi;

import io.micronaut.context.annotation.Requires;
import io.micronaut.mcp.annotations.Tool;
import io.micronaut.runtime.Micronaut;
import jakarta.inject.Singleton;

import java.util.Map;

/**
 * An MCP server over STDIO, started by {@link StdioAndHttpClientsTest} as a process.
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
                "spec.name", "StdioServer"))
            .start();
    }

    @Requires(property = "spec.name", value = "StdioServer")
    @Singleton
    static class StdioTools {
        @Tool(description = "Says hello from the STDIO server")
        String hello(String name) {
            return "Hello " + name + " from STDIO";
        }
    }
}
