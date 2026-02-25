package io.micronaut.mcp.server.exceptions;

import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JsonRrpcResponseUtilsTest {

    @Test
    void errorJsonrpcResponse() {
        McpSchema.JSONRPCRequest request = new McpSchema.JSONRPCRequest("2.0", "prompts/list", 3, null);
        McpError error = new McpError(new McpSchema.JSONRPCResponse.JSONRPCError(McpSchema.ErrorCodes.INTERNAL_ERROR, "Missing handler request type: prompts/list", null));
        McpSchema.JSONRPCResponse jsonrpcResponse = JsonRrpcResponseUtils.errorJsonrpcResponse(request, error);
        assertNotNull(jsonrpcResponse);
        assertNotNull(jsonrpcResponse.error());
    }
}
