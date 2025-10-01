package io.micronaut.mcp.client.langchain4j.http;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.mcp.client.McpClient;
import example.micronaut.moon.MoonPhase;
import example.micronaut.moon.MoonPhaseEmoji;
import io.micronaut.context.annotation.Property;
import io.micronaut.core.util.StringUtils;
import io.micronaut.json.JsonMapper;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Property(name = "micronaut.mcp.server.info.name", value = "mcp-server")
@Property(name = "micronaut.mcp.server.info.version", value = "0.0.1")
@Property(name = "micronaut.mcp.client.log-requests", value = StringUtils.TRUE)
@Property(name = "micronaut.mcp.client.log-response", value = StringUtils.TRUE)
@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "moon.enabled", value = StringUtils.TRUE)
@MicronautTest
class McpClientTest {
    @Test
    void testInjectMcpClient(McpClient client, JsonMapper jsonMapper) throws IOException {
        List<ToolSpecification> listToolsResult = assertDoesNotThrow(client::listTools);
        List<String> toolNames = listToolsResult.stream().map(ToolSpecification::name).toList();
        assertTrue(toolNames.stream().anyMatch(name -> name.equals("current-moon-phase")));
        assertTrue(toolNames.stream().anyMatch(name -> name.equals("moon-phase-at-date")));

        String arguments = jsonMapper.writeValueAsString(Map.of("date", "1982-10-28"));
        String json = assertDoesNotThrow(() -> client.executeTool(ToolExecutionRequest.builder()
            .name("moon-phase-at-date").arguments(arguments).build()));
        assertNotNull(json);
        MoonPhaseEmoji moonPhaseEmoji = jsonMapper.readValue(json, MoonPhaseEmoji.class);
        assertNotNull(moonPhaseEmoji);
        assertEquals(moonPhaseEmoji, new MoonPhaseEmoji(MoonPhase.WAXING_GIBBOUS, "\uD83C\uDF14"));
    }
}
