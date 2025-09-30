package io.micronaut.mcp.client.javasdk;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.json.JsonMapper;
import io.micronaut.mcp.annotations.Tool;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Property(name = "spec.name", value = "ToolReturnsEnumTest")
@MicronautTest
class ToolReturnsEnumTest {
    @Test
    void testInjectMcpSyncClient(McpSyncClient client, JsonMapper jsonMapper) {
        McpSchema.ListToolsResult listToolsResult = assertDoesNotThrow(() -> client.listTools());
        List<String> toolNames = listToolsResult.tools().stream().map(McpSchema.Tool::name).toList();
        assertTrue(toolNames.stream().anyMatch(name -> name.equals("sun-state")));

        McpSchema.CallToolResult callToolResult = assertDoesNotThrow(() -> client.callTool(McpSchema.CallToolRequest.builder()
            .name("sun-state")
            .build()));
        assertFalse(callToolResult.content().isEmpty());
        assertTrue(callToolResult.content().get(0) instanceof McpSchema.TextContent);
        McpSchema.TextContent textContent = (McpSchema.TextContent) callToolResult.content().get(0);
        assertEquals("TOTAL_ECLIPSE", textContent.text());
        assertNull(callToolResult.structuredContent());
    }

    @Requires(property = "spec.name", value = "ToolReturnsEnumTest")
    @Singleton
    static class Tools {
        @Tool(name = "sun-state")
        public SunState sunState() {
            return SunState.TOTAL_ECLIPSE;
        }
    }
}
