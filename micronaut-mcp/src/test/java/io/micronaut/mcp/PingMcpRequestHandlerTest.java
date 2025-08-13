package io.micronaut.mcp;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PingMcpRequestHandlerTest {

    @Test
    void nameIsPing() {
        PingMcpRequestHandler handler = new PingMcpRequestHandler();
        assertEquals("ping", handler.getName());
    }
}
