package io.micronaut.mcp;

import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CompositeMcpRequestHandlerTest {

    @Test
    void nameIsPing() {
        CompositeMcpRequestHandler handler = new CompositeMcpRequestHandler(Collections.emptyMap());
        assertEquals("composite", handler.getName());
    }
}
