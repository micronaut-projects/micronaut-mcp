package io.micronaut.mcp.server.registry;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.jsonschema.JsonSchema;
import io.micronaut.mcp.annotations.Tool;
import io.micronaut.mcp.annotations.ToolArg;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.server.McpStatelessServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A parameter of a type with a JSON Schema is the whole tool input, unless it is annotated with {@link ToolArg}, which
 * makes it a named property of the input.
 */
@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "spec.name", value = "ToolRegistryInputSchemaTest")
@MicronautTest(startApplication = false)
class ToolRegistryInputSchemaTest {
    private static final McpTransportContext CONTEXT = McpTransportContext.create(Map.of());

    @Inject
    ToolRegistry toolRegistry;

    @Test
    void jsonSchemaParameterIsTheWholeInput() {
        McpStatelessServerFeatures.SyncToolSpecification spec = spec("whole");
        assertEquals(Set.of("from", "to"), properties(spec.tool()).keySet());
        assertEquals("e2-e4", text(spec.callHandler().apply(CONTEXT,
            new McpSchema.CallToolRequest("whole", Map.of("from", "e2", "to", "e4")))));
    }

    @Test
    void toolArgParameterIsANamedProperty() {
        McpStatelessServerFeatures.SyncToolSpecification spec = spec("named");
        assertEquals(Set.of("move"), properties(spec.tool()).keySet());
        assertEquals(List.of("move"), spec.tool().inputSchema().get("required"));
        assertEquals("e2-e4", text(spec.callHandler().apply(CONTEXT,
            new McpSchema.CallToolRequest("named", Map.of("move", Map.of("from", "e2", "to", "e4"))))));
    }

    private McpStatelessServerFeatures.SyncToolSpecification spec(String name) {
        return toolRegistry.getStatelessSyncSpecs().stream().filter(s -> s.tool().name().equals(name)).findFirst().orElseThrow();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> properties(McpSchema.Tool tool) {
        return (Map<String, Object>) tool.inputSchema().get("properties");
    }

    private static String text(McpSchema.CallToolResult result) {
        return ((McpSchema.TextContent) result.content().getFirst()).text();
    }

    @JsonSchema
    @Introspected
    record Move(String from, String to) {
    }

    @Requires(property = "spec.name", value = "ToolRegistryInputSchemaTest")
    @Singleton
    static class Tools {
        @Tool
        String whole(Move move) {
            return move.from() + "-" + move.to();
        }

        @Tool
        String named(@ToolArg(description = "The move to play") Move move) {
            return move.from() + "-" + move.to();
        }
    }
}
