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
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Invokes the tool handlers of the four kinds of tool specifications: STDIO synchronous and asynchronous, and stateless
 * synchronous and asynchronous.
 */
@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "spec.name", value = "ToolRegistrySpecsTest")
@MicronautTest(startApplication = false)
class ToolRegistrySpecsTest {
    private static final McpTransportContext CONTEXT = McpTransportContext.create(Map.of());
    private static final McpAsyncServerExchange ASYNC_EXCHANGE = new McpAsyncServerExchange("session", null,
        McpSchema.ClientCapabilities.builder().build(), new McpSchema.Implementation("client", "1.0"), CONTEXT);
    private static final McpSyncServerExchange SYNC_EXCHANGE = new McpSyncServerExchange(ASYNC_EXCHANGE);
    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private static final EvaluationResult EVALUATION = new EvaluationResult("start", "+0.2");
    private static final McpSchema.CallToolResult CUSTOM_RESULT = McpSchema.CallToolResult.builder()
        .addTextContent("custom")
        .structuredContent(Map.of("custom", true))
        .isError(false)
        .build();

    @Inject
    ToolRegistry toolRegistry;

    enum Specs {
        SYNC, ASYNC, STATELESS_SYNC, STATELESS_ASYNC
    }

    @ParameterizedTest
    @EnumSource(Specs.class)
    void structuredResultIsPassedToTheSdkAsIs(Specs specs) {
        assertNotNull(tool(specs, "evaluate").outputSchema());
        McpSchema.CallToolResult result = call(specs, "evaluate");
        // With the Micronaut MCP JSON mapper the structured result is not converted
        assertSame(EVALUATION, result.structuredContent());
        assertFalse(result.isError());
    }

    @ParameterizedTest
    @EnumSource(Specs.class)
    void nullStructuredResultIsToolError(Specs specs) {
        McpSchema.CallToolResult result = call(specs, "missing");
        assertTrue(result.isError());
        assertNull(result.structuredContent());
        assertEquals("Tool missing produced no result. This is a server-side error; retrying with the same arguments will not help.",
            text(result));
    }

    @ParameterizedTest
    @EnumSource(Specs.class)
    void resultsWithoutOutputSchemaAreText(Specs specs) {
        assertNull(tool(specs, "echo").outputSchema());
        McpSchema.CallToolResult echo = call(specs, "echo", Map.of("text", "hi"));
        assertEquals("hi", text(echo));
        assertNull(echo.structuredContent());

        assertNull(tool(specs, "side").outputSchema());
        McpSchema.CallToolResult side = call(specs, "side");
        assertEquals("WHITE", text(side));
        assertNull(side.structuredContent());
    }

    @ParameterizedTest
    @EnumSource(Specs.class)
    void callToolResultIsReturnedAsIs(Specs specs) {
        assertSame(CUSTOM_RESULT, call(specs, "custom"));
    }

    private McpSchema.CallToolResult call(Specs specs, String name) {
        return call(specs, name, Map.of());
    }

    private McpSchema.CallToolResult call(Specs specs, String name, Map<String, Object> arguments) {
        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest(name, arguments);
        return switch (specs) {
            case SYNC -> toolRegistry.getSyncSpecs().stream().filter(s -> s.tool().name().equals(name)).findFirst().orElseThrow()
                .callHandler().apply(SYNC_EXCHANGE, request);
            case ASYNC -> toolRegistry.getAsyncSpecs().stream().filter(s -> s.tool().name().equals(name)).findFirst().orElseThrow()
                .callHandler().apply(ASYNC_EXCHANGE, request).block(TIMEOUT);
            case STATELESS_SYNC -> toolRegistry.getStatelessSyncSpecs().stream().filter(s -> s.tool().name().equals(name)).findFirst().orElseThrow()
                .callHandler().apply(CONTEXT, request);
            case STATELESS_ASYNC -> toolRegistry.getStatelessAsyncSpecs().stream().filter(s -> s.tool().name().equals(name)).findFirst().orElseThrow()
                .callHandler().apply(CONTEXT, request).block(TIMEOUT);
        };
    }

    private McpSchema.Tool tool(Specs specs, String name) {
        return switch (specs) {
            case SYNC -> toolRegistry.getSyncSpecs().stream().map(s -> s.tool()).filter(t -> t.name().equals(name)).findFirst().orElseThrow();
            case ASYNC -> toolRegistry.getAsyncSpecs().stream().map(s -> s.tool()).filter(t -> t.name().equals(name)).findFirst().orElseThrow();
            case STATELESS_SYNC -> toolRegistry.getStatelessSyncSpecs().stream().map(s -> s.tool()).filter(t -> t.name().equals(name)).findFirst().orElseThrow();
            case STATELESS_ASYNC -> toolRegistry.getStatelessAsyncSpecs().stream().map(s -> s.tool()).filter(t -> t.name().equals(name)).findFirst().orElseThrow();
        };
    }

    private static String text(McpSchema.CallToolResult result) {
        return ((McpSchema.TextContent) result.content().getFirst()).text();
    }

    enum Side {
        WHITE
    }

    @Requires(property = "spec.name", value = "ToolRegistrySpecsTest")
    @Singleton
    static class Tools {
        @Tool
        String echo(String text) {
            return text;
        }

        @Tool
        Side side() {
            return Side.WHITE;
        }

        @Tool
        EvaluationResult evaluate() {
            return EVALUATION;
        }

        @Tool
        @Nullable
        EvaluationResult missing() {
            return null;
        }

        @Tool
        McpSchema.CallToolResult custom() {
            return CUSTOM_RESULT;
        }
    }
}
