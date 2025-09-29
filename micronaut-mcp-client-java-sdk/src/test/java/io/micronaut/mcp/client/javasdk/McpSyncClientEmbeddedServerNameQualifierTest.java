package io.micronaut.mcp.client.javasdk;

import io.micronaut.context.annotation.Property;
import io.micronaut.core.util.StringUtils;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Named;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

@MicronautTest
class McpSyncClientEmbeddedServerNameQualifierTest {
    @Test
    void testInjectMcpSyncClientWithEmbeddedServerNameQualifier(@Named("embeddedServer") McpSyncClient client) {
        assertDoesNotThrow(client::initialize);
        McpSchema.ListToolsResult listToolsResult = assertDoesNotThrow(() -> client.listTools());
        List<String> toolNames = listToolsResult.tools().stream().map(McpSchema.Tool::name).toList();
        assertTrue(toolNames.stream().anyMatch(name -> name.equals("current-moon-phase")));
        assertTrue(toolNames.stream().anyMatch(name -> name.equals("moon-phase-at-date")));
    }

}
