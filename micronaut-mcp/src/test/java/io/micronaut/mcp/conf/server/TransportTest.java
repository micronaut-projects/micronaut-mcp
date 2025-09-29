package io.micronaut.mcp.conf.server;

import io.micronaut.mcp.conf.Transport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TransportTest {

    @Test
    void enumEqualsConstants() {
        Assertions.assertEquals(McpServerConfiguration.TRANSPORT_HTTP,  Transport.HTTP.name());
        assertEquals(McpServerConfiguration.TRANSPORT_STDIO,  Transport.STDIO.name());
    }
}
