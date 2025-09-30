package io.micronaut.mcp.client.javasdk;

import example.micronaut.moon.MoonPhase;
import example.micronaut.moon.MoonPhaseEmoji;
import io.micronaut.json.JsonMapper;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@MicronautTest
class McpSyncClientTest {
    @Test
    void testInjectMcpSyncClient(McpSyncClient client, JsonMapper jsonMapper) throws IOException {
        assertDoesNotThrow(client::initialize);
        McpSchema.ListToolsResult listToolsResult = assertDoesNotThrow(() -> client.listTools());
        List<String> toolNames = listToolsResult.tools().stream().map(McpSchema.Tool::name).toList();
        assertTrue(toolNames.stream().anyMatch(name -> name.equals("current-moon-phase")));
        assertTrue(toolNames.stream().anyMatch(name -> name.equals("moon-phase-at-date")));

        McpSchema.CallToolResult callToolResult = assertDoesNotThrow(() -> client.callTool(McpSchema.CallToolRequest.builder()
            .name("moon-phase-at-date")
            .arguments(Map.of("date", "1982-10-28"))
            .build()));
        assertFalse(callToolResult.content().isEmpty());
        assertTrue(callToolResult.content().get(0) instanceof McpSchema.TextContent);
        McpSchema.TextContent textContent = (McpSchema.TextContent) callToolResult.content().get(0);
        assertEquals("""
    {"phase":"WAXING_GIBBOUS","emoji":"\\uD83C\\uDF14"}""", textContent.text());
        String structuredContentJson = jsonMapper.writeValueAsString(callToolResult.structuredContent());
        MoonPhaseEmoji moonPhaseEmoji = jsonMapper.readValue(structuredContentJson, MoonPhaseEmoji.class);
        assertNotNull(moonPhaseEmoji);
        assertEquals(moonPhaseEmoji, new MoonPhaseEmoji(MoonPhase.WAXING_GIBBOUS, "\uD83C\uDF14"));
    }
}
