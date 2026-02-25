package io.micronaut.mcp.client.langchain4j.http;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.service.tool.ToolExecutionResult;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.util.StringUtils;
import io.micronaut.mcp.annotations.Tool;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Property(name = "micronaut.mcp.server.info.name", value = "mcp-server")
@Property(name = "micronaut.mcp.server.info.version", value = "0.0.1")
@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "spec.name", value = "ToolReturnsEnumTest")
@MicronautTest
class ToolReturnsEnumTest {
    @Test
    void testInjectMcpSyncClient(McpClient client) {
        List<ToolSpecification> listToolsResult = assertDoesNotThrow(() -> client.listTools());
        List<String> toolNames = listToolsResult.stream().map(ToolSpecification::name).toList();
        assertTrue(toolNames.stream().anyMatch(name -> name.equals("sun-state")));

        ToolExecutionResult toolExecutionResult = assertDoesNotThrow(() -> client.executeTool(ToolExecutionRequest.builder()
            .name("sun-state").build()));
        String text = toolExecutionResult.resultText();
        assertNotNull(text);
        assertEquals("TOTAL_ECLIPSE", text);
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
