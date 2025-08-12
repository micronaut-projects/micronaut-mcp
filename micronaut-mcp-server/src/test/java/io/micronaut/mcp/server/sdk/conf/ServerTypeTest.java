package io.micronaut.mcp.server.sdk.conf;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ServerTypeTest {

    @Test
    void enumEqualsConstants() {
        assertEquals(McpServerConfiguration.TYPE_SYNC,  ServerType.SYNC.name());
        assertEquals(McpServerConfiguration.TYPE_ASYNC,  ServerType.ASYNC.name());
        assertEquals(McpServerConfiguration.TYPE_STATELESS_SYNC,  ServerType.STATELESS_SYNC.name());
        assertEquals(McpServerConfiguration.TYPE_STATELESS_ASYNC,  ServerType.STATELESS_ASYNC.name());
    }
}
