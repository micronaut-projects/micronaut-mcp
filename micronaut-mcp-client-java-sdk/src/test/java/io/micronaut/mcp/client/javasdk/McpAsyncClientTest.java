package io.micronaut.mcp.client.javasdk;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

@MicronautTest
class McpAsyncClientTest {
    @Test
    void testInjectMcpAsyncClient(McpAsyncClient client) {
        assertDoesNotThrow(() -> client.initialize().block());
        McpSchema.ListToolsResult listToolsResult = assertDoesNotThrow(() -> client.listTools().block());
        List<String> toolNames = listToolsResult.tools().stream().map(McpSchema.Tool::name).toList();
        assertTrue(toolNames.stream().anyMatch(name -> name.equals("current-moon-phase")));
        assertTrue(toolNames.stream().anyMatch(name -> name.equals("moon-phase-at-date")));
    }
}
