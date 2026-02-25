/*
 * Copyright 2017-2025 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.mcp.server.exceptions;

import io.micronaut.core.annotation.Internal;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;

import java.util.Collections;

import static io.modelcontextprotocol.spec.McpSchema.JSONRPC_VERSION;

/**
 * Utility class to generate {@link McpSchema.JSONRPCResponse}s allowing the gracious handling of MCP errors for primitive listings.
 */
@Internal
public final class JsonRrpcResponseUtils {
    private static final String HANDLER_FOR_REQUEST_TYPE_PROMPTS_LIST = "Missing handler for request type: prompts/list";
    private static final String HANDLER_FOR_REQUEST_TYPE_RESOURCES_LIST = "Missing handler for request type: resources/list";
    private static final String HANDLER_FOR_REQUEST_TYPE_RESOURCES_TEMPLATES_LIST = "Missing handler for request type: resources/templates/list";
    private static final String HANDLER_FOR_REQUEST_TYPE_TOOLS_LIST = "Missing handler for request type: tools/list";
    private static final String METHOD_NOT_FOUND_PROMPTS_LIST = "Method not found: prompts/list";
    private static final String METHOD_NOT_FOUND_RESOURCES_LIST = "Method not found: resources/list";
    private static final String METHOD_NOT_FOUND_RESOURCES_TEMPLATES_LIST = "Method not found: resources/templates/list";
    private static final String METHOD_NOT_FOUND_TOOLS_LIST = "Method not found: tools/list";

    private JsonRrpcResponseUtils() {
    }

    /**
     * If the MCP error is related to a listing request to a primitive, it returns an empty response for that primitive instead of an error.
     *
     * @param e              MCP Error
     * @param jsonrpcMessage JSON RPC Message
     * @return a JSON RPC Response
     */
    @NonNull
    public static McpSchema.JSONRPCResponse jsonrpcResponse(@NonNull McpError e, @NonNull McpSchema.JSONRPCMessage jsonrpcMessage) {
        if (e.getMessage() == null) {
            return errorJsonrpcResponse(jsonrpcMessage, e);
        }
        switch (e.getMessage()) {
            case HANDLER_FOR_REQUEST_TYPE_PROMPTS_LIST -> {
                Object id = jsonrpcMessage instanceof McpSchema.JSONRPCRequest jsonrpcRequest ? jsonrpcRequest.id() : null;
                return new McpSchema.JSONRPCResponse(JSONRPC_VERSION,
                    id,
                    new McpSchema.ListPromptsResult(Collections.emptyList(), null), null);
            }
            case HANDLER_FOR_REQUEST_TYPE_RESOURCES_LIST -> {
                Object id = jsonrpcMessage instanceof McpSchema.JSONRPCRequest jsonrpcRequest ? jsonrpcRequest.id() : null;
                return new McpSchema.JSONRPCResponse(JSONRPC_VERSION,
                    id,
                    new McpSchema.ListResourcesResult(Collections.emptyList(), null), null);
            }
            case HANDLER_FOR_REQUEST_TYPE_RESOURCES_TEMPLATES_LIST -> {
                Object id = jsonrpcMessage instanceof McpSchema.JSONRPCRequest jsonrpcRequest ? jsonrpcRequest.id() : null;
                return new McpSchema.JSONRPCResponse(JSONRPC_VERSION,
                    id,
                    new McpSchema.ListResourceTemplatesResult(Collections.emptyList(), null), null);
            }
            case HANDLER_FOR_REQUEST_TYPE_TOOLS_LIST -> {
                Object id = jsonrpcMessage instanceof McpSchema.JSONRPCRequest jsonrpcRequest ? jsonrpcRequest.id() : null;
                return new McpSchema.JSONRPCResponse(JSONRPC_VERSION,
                    id,
                    new McpSchema.ListToolsResult(Collections.emptyList(), null), null);
            }
            default -> errorJsonrpcResponse(jsonrpcMessage, e);
        }
        return errorJsonrpcResponse(jsonrpcMessage, e);
    }


    /**
     * If the JSONRPCResponse signals an error and the error is related to a listing request to a primitive, it returns an empty response for that primitive instead of an error.
     *
     * @param jsonrpcResponse JSON RPC Response
     * @return a JSON RPC Response
     */
    @Nullable
    public static McpSchema.JSONRPCMessage map(@Nullable McpSchema.JSONRPCResponse jsonrpcResponse) {
        if (jsonrpcResponse == null) {
            return null;
        }
        if (jsonrpcResponse.error() == null) {
            return jsonrpcResponse;
        }
        McpSchema.JSONRPCResponse.JSONRPCError jsonrpcError = jsonrpcResponse.error();
        String message = jsonrpcError.message();
        if (message == null) {
            return jsonrpcResponse;
        }
        return switch (message) {
            case METHOD_NOT_FOUND_PROMPTS_LIST -> new McpSchema.JSONRPCResponse(JSONRPC_VERSION,
                jsonrpcResponse.id(),
                new McpSchema.ListPromptsResult(Collections.emptyList(), null), null);
            case METHOD_NOT_FOUND_RESOURCES_LIST -> new McpSchema.JSONRPCResponse(JSONRPC_VERSION,
                jsonrpcResponse.id(),
                new McpSchema.ListResourcesResult(Collections.emptyList(), null), null);
            case METHOD_NOT_FOUND_RESOURCES_TEMPLATES_LIST ->
                new McpSchema.JSONRPCResponse(JSONRPC_VERSION,
                    jsonrpcResponse.id(),
                    new McpSchema.ListResourceTemplatesResult(Collections.emptyList(), null), null);
            case METHOD_NOT_FOUND_TOOLS_LIST -> new McpSchema.JSONRPCResponse(JSONRPC_VERSION,
                jsonrpcResponse.id(),
                new McpSchema.ListToolsResult(Collections.emptyList(), null), null);
            default -> jsonrpcResponse;
        };
    }

    @NonNull
    static McpSchema.JSONRPCResponse errorJsonrpcResponse(@NonNull McpSchema.JSONRPCMessage jsonrpcMessage,
                                                          @NonNull McpError error) {
        McpSchema.JSONRPCResponse.JSONRPCError jsonrpcError = error.getJsonRpcError();
        if (jsonrpcError == null) {
            jsonrpcError = new McpSchema.JSONRPCResponse.JSONRPCError(McpSchema.ErrorCodes.INTERNAL_ERROR,
                error.getMessage(),
                null);
        }
        return new McpSchema.JSONRPCResponse(
            JSONRPC_VERSION,
            jsonrpcMessage instanceof McpSchema.JSONRPCRequest jsonrpcRequest ? jsonrpcRequest.id() : null,
            null,
            jsonrpcError
        );
    }
}
