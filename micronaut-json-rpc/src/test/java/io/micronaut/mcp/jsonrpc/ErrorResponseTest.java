package io.micronaut.mcp.jsonrpc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ErrorResponseTest {

    @Test
    void testErrorResponse() {
        var response = new ErrorResponse<>(new Error<>(ErrorCode.METHOD_NOT_FOUND), "1");
        assertEquals("1", response.id());
        assertEquals(new Error<>(ErrorCode.METHOD_NOT_FOUND), response.error());
        assertEquals("2.0", response.jsonrpc());

        response = new ErrorResponse<>(new Error<>(ErrorCode.METHOD_NOT_FOUND));
        assertNull(response.id());
        assertEquals(new Error<>(ErrorCode.METHOD_NOT_FOUND), response.error());
        assertEquals("2.0", response.jsonrpc());
    }
}
