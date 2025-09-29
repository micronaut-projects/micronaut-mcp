package io.micronaut.mcp.client.javasdk;

import io.micronaut.context.ApplicationContext;
import io.micronaut.core.util.StringUtils;
import io.micronaut.inject.qualifiers.Qualifiers;
import io.micronaut.runtime.server.EmbeddedServer;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class McpSyncClientViaConfigurationTest {

    @Test
    void mcpSyncClientViaConfigurationTest() {
        Map<String, Object> mcpServerConfig = Map.of("moon.enabled", StringUtils.TRUE);
        try (EmbeddedServer mcpServer = ApplicationContext.run(EmbeddedServer.class, mcpServerConfig)) {
            Map<String, Object> serverConfig = Map.of("micronaut.mcp.client.http.remotemcpserver.url", mcpServer.getURL().toString());
            try (EmbeddedServer server = ApplicationContext.run(EmbeddedServer.class, serverConfig)) {
                Assertions.assertTrue(server.getApplicationContext().containsBean(McpClientHtttpConfiguration.class, Qualifiers.byName("remotemcpserver")));
                Assertions.assertTrue(server.getApplicationContext().containsBean(McpClientHtttpConfiguration.class, Qualifiers.byName("remotemcpserver")));
                Assertions.assertTrue(server.getApplicationContext().containsBean(HttpClientStreamableHttpTransport.class, Qualifiers.byName("remotemcpserver")));

                McpSyncClient client = server.getApplicationContext().getBean(McpSyncClient.class, Qualifiers.byName("remotemcpserver"));
                McpSchema.ListToolsResult listToolsResult = assertDoesNotThrow(() -> client.listTools());
                List<String> toolNames = listToolsResult.tools().stream().map(McpSchema.Tool::name).toList();
                assertTrue(toolNames.stream().anyMatch(name -> name.equals("current-moon-phase")));
                assertTrue(toolNames.stream().anyMatch(name -> name.equals("moon-phase-at-date")));
            }
        }
    }
}
