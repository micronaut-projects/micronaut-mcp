package io.micronaut.mcp.server.stateless;

import io.micronaut.http.HttpStatus;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class McpControllerTest {

    static Stream<Arguments> errorCodesAndExpectedStatuses() {
        return Stream.of(
            Arguments.of(McpSchema.ErrorCodes.PARSE_ERROR, HttpStatus.BAD_REQUEST),
            Arguments.of(McpSchema.ErrorCodes.INVALID_REQUEST, HttpStatus.BAD_REQUEST),
            Arguments.of(McpSchema.ErrorCodes.METHOD_NOT_FOUND, HttpStatus.BAD_REQUEST),
            Arguments.of(McpSchema.ErrorCodes.INVALID_PARAMS, HttpStatus.BAD_REQUEST),
            Arguments.of(McpSchema.ErrorCodes.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR),
            // Unknown/unspecified error codes should default to 500
            Arguments.of(42, HttpStatus.INTERNAL_SERVER_ERROR)
        );
    }

    @ParameterizedTest
    @MethodSource("errorCodesAndExpectedStatuses")
    void statusMapsJsonRpcErrorCodeToHttpStatus(int errorCode, HttpStatus expected) {
        McpSchema.JSONRPCResponse.JSONRPCError jsonRpcError = McpError
            .builder(errorCode)
            .message("test")
            .build()
            .getJsonRpcError();

        HttpStatus actual = McpController.status(jsonRpcError);
        assertEquals(expected, actual);
    }
}
