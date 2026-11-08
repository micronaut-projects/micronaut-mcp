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
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Invokes the tool handlers of the STDIO synchronous and the stateless asynchronous specifications.
 */
@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "spec.name", value = "ToolRegistrySpecsTest")
@MicronautTest(startApplication = false)
class ToolRegistrySpecsTest {
    private static final McpTransportContext CONTEXT = McpTransportContext.create(Map.of());
    private static final McpSyncServerExchange EXCHANGE = new McpSyncServerExchange(new McpAsyncServerExchange("session", null,
        McpSchema.ClientCapabilities.builder().build(), new McpSchema.Implementation("client", "1.0"), CONTEXT));

    @Inject
    ToolRegistry toolRegistry;

    @Test
    void synchronousSpecificationsCallTheTools() {
        McpServerFeaturesSync specs = new McpServerFeaturesSync(toolRegistry);
        assertEquals("hi", text(specs.call("echo", Map.of("text", "hi"))));
        // With the Micronaut MCP JSON mapper the structured result is passed to the SDK as is
        assertEquals(new EvaluationResult("start", "+0.2"), specs.call("evaluate", Map.of()).structuredContent());
    }

    @Test
    void statelessAsynchronousSpecificationsCallTheTools() {
        assertEquals("hi", text(statelessAsync("echo", Map.of("text", "hi"))));
        McpSchema.CallToolResult result = statelessAsync("evaluate", Map.of());
        assertNotNull(result.structuredContent());
        assertEquals(false, result.isError());
    }

    private McpSchema.CallToolResult statelessAsync(String name, Map<String, Object> arguments) {
        return toolRegistry.getStatelessAsyncSpecs().stream().filter(s -> s.tool().name().equals(name)).findFirst().orElseThrow()
            .callHandler().apply(CONTEXT, new McpSchema.CallToolRequest(name, arguments)).block(Duration.ofSeconds(10));
    }

    private static String text(McpSchema.CallToolResult result) {
        return ((McpSchema.TextContent) result.content().getFirst()).text();
    }

    private record McpServerFeaturesSync(ToolRegistry registry) {
        McpSchema.CallToolResult call(String name, Map<String, Object> arguments) {
            return registry.getSyncSpecs().stream().filter(s -> s.tool().name().equals(name)).findFirst().orElseThrow()
                .callHandler().apply(EXCHANGE, new McpSchema.CallToolRequest(name, arguments));
        }
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
            return new EvaluationResult("start", "+0.2");
        }
    }
}
