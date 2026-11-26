package io.micronaut.mcp.server.registry;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.mcp.annotations.Tool;
import io.micronaut.mcp.server.stateless.sync.EvaluationResult;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.server.McpAsyncServerExchange;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Invokes the tool handlers of the STDIO synchronous and asynchronous specifications and of the stateless asynchronous specifications.
 */
@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "spec.name", value = "ToolRegistrySpecsTest")
@MicronautTest(startApplication = false)
class ToolRegistrySpecsTest {
    private static final McpTransportContext CONTEXT = McpTransportContext.create(Map.of());
    private static final McpAsyncServerExchange ASYNC_EXCHANGE = new McpAsyncServerExchange("session", null,
        McpSchema.ClientCapabilities.builder().build(), new McpSchema.Implementation("client", "1.0"), CONTEXT);
    private static final McpSyncServerExchange SYNC_EXCHANGE = new McpSyncServerExchange(ASYNC_EXCHANGE);
    private static final EvaluationResult EVALUATION = new EvaluationResult("start", "+0.2");

    @Inject
    ToolRegistry toolRegistry;

    @Test
    void synchronousSpecificationsCallTheTools() {
        assertEquals("hi", text(sync("echo", Map.of("text", "hi"))));
        // With the Micronaut MCP JSON mapper the structured result is passed to the SDK as is
        assertStructured(sync("evaluate", Map.of()));
    }

    @Test
    void asynchronousSpecificationsCallTheTools() {
        assertEquals("hi", text(async("echo", Map.of("text", "hi"))));
        assertStructured(async("evaluate", Map.of()));
    }

    @Test
    void statelessAsynchronousSpecificationsCallTheTools() {
        assertEquals("hi", text(statelessAsync("echo", Map.of("text", "hi"))));
        assertStructured(statelessAsync("evaluate", Map.of()));
    }

    private McpSchema.CallToolResult sync(String name, Map<String, Object> arguments) {
        return toolRegistry.getSyncSpecs().stream().filter(s -> s.tool().name().equals(name)).findFirst().orElseThrow()
            .callHandler().apply(SYNC_EXCHANGE, new McpSchema.CallToolRequest(name, arguments));
    }

    private McpSchema.CallToolResult async(String name, Map<String, Object> arguments) {
        return toolRegistry.getAsyncSpecs().stream().filter(s -> s.tool().name().equals(name)).findFirst().orElseThrow()
            .callHandler().apply(ASYNC_EXCHANGE, new McpSchema.CallToolRequest(name, arguments)).block(Duration.ofSeconds(10));
    }

    private McpSchema.CallToolResult statelessAsync(String name, Map<String, Object> arguments) {
        return toolRegistry.getStatelessAsyncSpecs().stream().filter(s -> s.tool().name().equals(name)).findFirst().orElseThrow()
            .callHandler().apply(CONTEXT, new McpSchema.CallToolRequest(name, arguments)).block(Duration.ofSeconds(10));
    }

    private static void assertStructured(McpSchema.CallToolResult result) {
        assertEquals(EVALUATION, result.structuredContent());
        assertFalse(result.isError());
    }

    private static String text(McpSchema.CallToolResult result) {
        return ((McpSchema.TextContent) result.content().getFirst()).text();
    }

    @Requires(property = "spec.name", value = "ToolRegistrySpecsTest")
    @Singleton
    static class Tools {
        @Tool
        String echo(String text) {
            return text;
        }

        @Tool
        EvaluationResult evaluate() {
            return EVALUATION;
        }
    }
}
