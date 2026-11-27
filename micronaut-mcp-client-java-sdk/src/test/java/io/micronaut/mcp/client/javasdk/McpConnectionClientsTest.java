package io.micronaut.mcp.client.javasdk;

import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.client.McpSyncClient;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class McpConnectionClientsTest {
    @Test
    void lateCleanupLeavesTheReplacementClientAndItsCatalogIntact() {
        McpConnectionClients clients = new McpConnectionClients();
        var first = clients.claim("weather", McpSyncClient.class);
        var replacement = clients.claim("weather", McpSyncClient.class);
        clients.toolsChanged("weather", List.of());
        clients.release("weather", first);
        assertEquals(List.of(), clients.tools("weather"));
        assertThrows(IllegalStateException.class, () -> clients.claim("weather", McpAsyncClient.class));
        clients.release("weather", replacement);
        assertDoesNotThrow(() -> clients.claim("weather", McpAsyncClient.class));
    }
}
