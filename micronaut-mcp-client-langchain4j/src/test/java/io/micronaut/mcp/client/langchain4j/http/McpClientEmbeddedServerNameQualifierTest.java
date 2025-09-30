package io.micronaut.mcp.client.langchain4j.http;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.mcp.client.McpClient;
import io.micronaut.context.annotation.Property;
import io.micronaut.core.util.StringUtils;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Named;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Property(name = "micronaut.mcp.server.info.name", value = "mcp-server")
@Property(name = "micronaut.mcp.server.info.version", value = "0.0.1")
@Property(name = "micronaut.mcp.client.log-requests", value = StringUtils.TRUE)
@Property(name = "micronaut.mcp.client.log-response", value = StringUtils.TRUE)
@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "moon.enabled", value = StringUtils.TRUE)
@MicronautTest
class McpClientEmbeddedServerNameQualifierTest {
    @Test
    void testInjectMcpClient(@Named("embeddedServer") McpClient client) {
        List<ToolSpecification> listToolsResult = assertDoesNotThrow(client::listTools);
        List<String> toolNames = listToolsResult.stream().map(ToolSpecification::name).toList();
        assertTrue(toolNames.stream().anyMatch(name -> name.equals("current-moon-phase")));
        assertTrue(toolNames.stream().anyMatch(name -> name.equals("moon-phase-at-date")));
    }
}
