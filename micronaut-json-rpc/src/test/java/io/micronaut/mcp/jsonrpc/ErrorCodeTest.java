package io.micronaut.mcp.jsonrpc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ErrorCodeTest{
    @Test
    void testOfKnownValues() {
        assertAll(
            () -> {
                ErrorCode code = ErrorCode.PARSE_ERROR;
                assertEquals(code, ErrorCode.of(-32700));
                assertEquals("Parse error", code.getMessage());
                assertEquals("Invalid JSON was received by the server. An error occurred on the server while parsing the JSON text.", code.getMeaning());
            },
            () -> {
                int codeNumber = -32600;
                ErrorCode code = ErrorCode.INVALID_REQUEST;
                assertEquals(code, ErrorCode.of(codeNumber));
                assertEquals("Invalid Request", code.getMessage());
                assertEquals("The JSON sent is not a valid Request object.", code.getMeaning());
                assertEquals(codeNumber, code.getCode());
            },
            () -> {
                ErrorCode code = ErrorCode.METHOD_NOT_FOUND;
                assertEquals(code, ErrorCode.of(-32601));
                assertEquals("Method not found", code.getMessage());
                assertEquals("The method does not exist / is not available.", code.getMeaning());
            },
            () -> {
                ErrorCode code = ErrorCode.INVALID_PARAMS;
                assertEquals(code, ErrorCode.of(-32602));
                assertEquals("Invalid params", code.getMessage());
                assertEquals("Invalid method parameter(s).", code.getMeaning());
            },
            () -> {
                ErrorCode code = ErrorCode.INTERNAL_ERROR;
                assertEquals(code, ErrorCode.of(-32603));
                assertEquals("Internal error", code.getMessage());
                assertEquals("Internal JSON-RPC error.", code.getMeaning());
            },
            () -> {
                ErrorCode code = ErrorCode.SERVER_ERROR;
                assertEquals(code, ErrorCode.of(-32000));
                assertEquals("Server error", code.getMessage());
                assertEquals("Reserved for implementation-defined server-errors.", code.getMeaning());
            }
        );
    }

    @Test
    void unknownErrorCode() {
        assertNull(ErrorCode.of(8080));
    }
}
