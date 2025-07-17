package io.micronaut.mcp.jsonrpc;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ErrorTest {

    @Test
    void errorWithCode() {
        var error = new Error<>(ErrorCode.METHOD_NOT_FOUND);
        assertEquals(-32601, error.code());
        assertEquals("Method not found", error.message());
        assertNull(error.data());
    }
}
