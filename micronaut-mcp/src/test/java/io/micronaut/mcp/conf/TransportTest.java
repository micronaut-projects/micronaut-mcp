package io.micronaut.mcp.conf;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TransportTest {

    @Test
    void enumEqualsConstants() {
        assertEquals(McpServerConfiguration.TRANSPORT_HTTP,  Transport.HTTP.name());
        assertEquals(McpServerConfiguration.TRANSPORT_STDIO,  Transport.STDIO.name());
    }
}
