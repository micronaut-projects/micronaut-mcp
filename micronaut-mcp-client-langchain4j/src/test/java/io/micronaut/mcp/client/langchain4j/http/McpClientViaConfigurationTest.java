package io.micronaut.mcp.client.langchain4j.http;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.mcp.client.McpClient;
import io.micronaut.context.ApplicationContext;
import io.micronaut.core.util.StringUtils;
import io.micronaut.inject.qualifiers.Qualifiers;
import io.micronaut.mcp.conf.client.McpClientHttpConfiguration;
import io.micronaut.mcp.conf.server.McpServerConfiguration;
import io.micronaut.runtime.server.EmbeddedServer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpClientViaConfigurationTest {

    @Test
    void mcpClientViaConfigurationTest() {
        Map<String, Object> mcpServerConfig = Map.of(
            "moon.enabled", StringUtils.TRUE,
            "micronaut.mcp.server.info.version", "1.0.0",
            "micronaut.mcp.server.info.name", "Micronaut MCP Weather Demo",
            "micronaut.mcp.server.transport", "HTTP"
            );
        try (EmbeddedServer mcpServer = ApplicationContext.run(EmbeddedServer.class, mcpServerConfig)) {
            Map<String, Object> serverConfig = Map.of("micronaut.mcp.client.http.remotemcpserver.url", mcpServer.getURL().toString()
                + mcpServer.getApplicationContext().getBean(McpServerConfiguration.class).getEndpoint());
            try (EmbeddedServer server = ApplicationContext.run(EmbeddedServer.class, serverConfig)) {
                Assertions.assertTrue(server.getApplicationContext().containsBean(McpClientHttpConfiguration.class, Qualifiers.byName("remotemcpserver")));
                McpClient client = server.getApplicationContext().getBean(McpClient.class, Qualifiers.byName("remotemcpserver"));
                List<ToolSpecification> listToolsResult = assertDoesNotThrow(() -> client.listTools());
                List<String> toolNames = listToolsResult.stream().map(ToolSpecification::name).toList();
                assertTrue(toolNames.stream().anyMatch(name -> name.equals("current-moon-phase")));
                assertTrue(toolNames.stream().anyMatch(name -> name.equals("moon-phase-at-date")));
            }
        }
    }
}
