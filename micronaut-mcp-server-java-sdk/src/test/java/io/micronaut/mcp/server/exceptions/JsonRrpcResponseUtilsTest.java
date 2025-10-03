package io.micronaut.mcp.server.exceptions;

import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JsonRrpcResponseUtilsTest {

    @Test
    void errorJsonrpcResponse() {
        McpSchema.JSONRPCRequest request = new McpSchema.JSONRPCRequest("2.0", "prompts/list", 3, null);
        McpError error = new McpError("Missing handler request type: prompts/list");
        McpSchema.JSONRPCResponse jsonrpcResponse = JsonRrpcResponseUtils.errorJsonrpcResponse(request, error);
        assertNotNull(jsonrpcResponse);
        assertNotNull(jsonrpcResponse.error());
    }
}
