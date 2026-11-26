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
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

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
        assertEquals("e2-e4", call(spec, Map.of("from", "e2", "to", "e4")));
    }

    @Test
    void toolArgParameterIsANamedProperty() {
        McpStatelessServerFeatures.SyncToolSpecification spec = spec("named");
        assertEquals(Set.of("move"), properties(spec.tool()).keySet());
        Map<String, Object> move = property(spec.tool(), "move");
        assertEquals("object", move.get("type"));
        assertEquals("The move to play", move.get("description"));
        assertEquals(Map.of("from", Map.of("type", "string"), "to", Map.of("type", "string")), move.get("properties"));
        // The embedded schema keeps its identifier, so that it stays a schema resource of its own, and loses its dialect
        assertEquals("classpath:META-INF/schemas/tool-registry-input-schema-test-move.schema.json", move.get("$id"));
        assertFalse(move.containsKey("$schema"));
        assertEquals(List.of("move"), spec.tool().inputSchema().get("required"));
        assertEquals("e2-e4", call(spec, Map.of("move", Map.of("from", "e2", "to", "e4"))));
    }

    @Test
    void toolArgParameterIsAPropertyNextToTheOtherParameters() {
        McpStatelessServerFeatures.SyncToolSpecification spec = spec("annotate");
        assertEquals(Set.of("move", "comment"), properties(spec.tool()).keySet());
        assertEquals("string", ((McpSchema.JsonSchema) properties(spec.tool()).get("comment")).type());
        // Without a description in ToolArg, the embedded schema has none
        assertFalse(property(spec.tool(), "move").containsKey("description"));
        assertEquals(List.of("move", "comment"), spec.tool().inputSchema().get("required"));
        assertEquals("e2-e4!", call(spec, Map.of("move", Map.of("from", "e2", "to", "e4"), "comment", "!")));
    }

    @Test
    void optionalToolArgParameterIsNotRequired() {
        McpStatelessServerFeatures.SyncToolSpecification spec = spec("optional");
        assertEquals(Set.of("move"), properties(spec.tool()).keySet());
        assertEquals(List.of(), spec.tool().inputSchema().get("required"));
        assertEquals("no move", call(spec, Map.of()));
    }

    @Test
    void nestedTypesOfAnEmbeddedSchemaKeepTheirAbsoluteReferences() {
        McpStatelessServerFeatures.SyncToolSpecification spec = spec("line");
        @SuppressWarnings("unchecked")
        Map<String, Object> lineProperties = (Map<String, Object>) property(spec.tool(), "line").get("properties");
        // The generated references are absolute
        assertEquals(Map.of("$ref", "classpath:META-INF/schemas/tool-registry-input-schema-test-move.schema.json"),
            lineProperties.get("first"));
        assertEquals("e2", call(spec, Map.of("line", Map.of(
            "first", Map.of("from", "e2", "to", "e4"),
            "second", Map.of("from", "e7", "to", "e5")))));
    }

    private static String call(McpStatelessServerFeatures.SyncToolSpecification spec, Map<String, Object> arguments) {
        return text(spec.callHandler().apply(CONTEXT, new McpSchema.CallToolRequest(spec.tool().name(), arguments)));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> property(McpSchema.Tool tool, String name) {
        return (Map<String, Object>) properties(tool).get(name);
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

    @JsonSchema
    @Introspected
    record Line(Move first, Move second) {
    }

    @Requires(property = "spec.name", value = "ToolRegistryInputSchemaTest")
    @Singleton
    static class Tools {
        @Tool
        String whole(Move move) {
            return move.from() + "-" + move.to();
        }

        @Tool
        String line(@ToolArg Line line) {
            return line.first().from();
        }

        @Tool
        String named(@ToolArg(description = "The move to play") Move move) {
            return move.from() + "-" + move.to();
        }

        @Tool
        String annotate(@ToolArg Move move, String comment) {
            return move.from() + "-" + move.to() + comment;
        }

        @Tool
        String optional(@ToolArg @Nullable Move move) {
            return move == null ? "no move" : move.from();
        }
    }
}
