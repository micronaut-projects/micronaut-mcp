package io.micronaut.mcp.jsonrpc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


class SuccessfulResponseTest {

    @Test
    void successfulResponse() {
        var response = new SuccessfulResponse<>(19, 1);
        assertEquals(1, response.id());
        assertEquals(19, response.result());
        assertEquals("2.0", response.jsonrpc());
    }
}
