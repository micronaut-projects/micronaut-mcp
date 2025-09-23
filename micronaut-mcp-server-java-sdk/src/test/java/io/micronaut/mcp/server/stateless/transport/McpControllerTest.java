package io.micronaut.mcp.server.stateless.transport;

import io.micronaut.http.HttpStatus;
import io.micronaut.mcp.server.stateless.transport.HttpServerMcpStatelessServerTransport;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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

        int actual = HttpServerMcpStatelessServerTransport.status(jsonRpcError);
        assertEquals(expected.getCode(), actual);
    }

    @Test
    void errorJsonrpcResponse() {
        McpSchema.JSONRPCRequest request = new McpSchema.JSONRPCRequest("2.0", "prompts/list", 3, null);
        McpError error = new McpError("Missing handler request type: prompts/list");
        McpSchema.JSONRPCResponse jsonrpcResponse = HttpServerMcpStatelessServerTransport.errorJsonrpcResponse(request, error);
        assertNotNull(jsonrpcResponse);
        assertNotNull(jsonrpcResponse.error());
    }
}
