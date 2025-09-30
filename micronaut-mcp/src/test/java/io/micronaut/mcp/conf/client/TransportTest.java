package io.micronaut.mcp.conf.client;

import io.micronaut.mcp.conf.Transport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TransportTest {

    @Test
    void enumEqualsConstants() {
        Assertions.assertEquals(McpClientConfiguration.TRANSPORT_HTTP,  Transport.HTTP.name());
        assertEquals(McpClientConfiguration.TRANSPORT_STDIO,  Transport.STDIO.name());
    }
}
