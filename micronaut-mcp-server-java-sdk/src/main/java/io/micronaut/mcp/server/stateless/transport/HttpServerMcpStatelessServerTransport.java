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
package io.micronaut.mcp.server.stateless.transport;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.server.McpStatelessServerHandler;
import io.modelcontextprotocol.server.McpTransportContextExtractor;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpStatelessServerTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.function.Function;

import static io.modelcontextprotocol.spec.McpSchema.JSONRPC_VERSION;

/**
 *
 * @param <T> Request Type
 */
public class HttpServerMcpStatelessServerTransport<T> implements McpStatelessServerTransport {
    private static final String KEY_METHOD = "method";
    private static final String KEY_ID = "id";
    private static final String KEY_JSONRPC = "jsonrpc";
    private static final String KEY_PARAMS = "params";

    private static final Logger LOG = LoggerFactory.getLogger(HttpServerMcpStatelessServerTransport.class);
    private final McpTransportContextExtractor<T> contextExtractor;
    private McpStatelessServerHandler mcpHandler;

    public HttpServerMcpStatelessServerTransport(@NonNull McpTransportContextExtractor<T> contextExtractor) {
        this.contextExtractor = contextExtractor;
    }

    @Override
    public void setMcpHandler(McpStatelessServerHandler mcpHandler) {
        this.mcpHandler = mcpHandler;
    }

    /**
     * Handle POST request to MCP Endpoint.
     * @param request HTTP Request
     * @param body HTTP Request Body
     * @return HTTP Response
     */
    @NonNull
    public Mono<HttpJsonRpcResponse> handlePost(@NonNull T request, @NonNull Map<String, Object> body) {
            McpTransportContext transportContext = contextExtractor.extract(request);
        McpSchema.JSONRPCMessage jsonRpcMessage = jsonRpcMessage(body);
        if (jsonRpcMessage instanceof McpSchema.JSONRPCRequest jsonrpcRequest) {
            return handleJsonRpcRequest(jsonrpcRequest, transportContext);
        } else if (jsonRpcMessage instanceof McpSchema.JSONRPCNotification notification) {
            return handleJsonRpcNotification(notification, transportContext);
        }
        throw mcpError(McpSchema.ErrorCodes.INVALID_REQUEST, "The server accepts either requests or notifications");
    }

    @Override
    public Mono<Void> closeGracefully() {
        return Mono.empty();
    }

    @SuppressWarnings("java:S3740")
    private Mono<HttpJsonRpcResponse> handleJsonRpcNotification(McpSchema.JSONRPCNotification jsonrpcNotification, McpTransportContext transportContext) {

        Mono<McpSchema.JSONRPCResponse> jsonrpcResponse = mcpHandler.handleNotification(transportContext, jsonrpcNotification)
            .then(Mono.fromCallable(() -> {
                McpSchema.JSONRPCResponse response = null;
                return response;
            }));
        return handleJsonRPCResponse(jsonrpcNotification, transportContext, jsonrpcResponse, rsp -> {
            int status = status(rsp);
            if (status >= 400) {
                return new HttpJsonRpcResponse(status, rsp);
            }
            return new HttpJsonRpcResponse(202, null);
        });
    }

    @SuppressWarnings("java:S3740")
    @NonNull
    private Mono<HttpJsonRpcResponse> handleJsonRpcRequest(@NonNull McpSchema.JSONRPCRequest jsonrpcRequest,
                                                           @NonNull McpTransportContext transportContext) {
        Mono<McpSchema.JSONRPCResponse> jsonrpcResponse = mcpHandler.handleRequest(transportContext, jsonrpcRequest);
        return handleJsonRPCResponse(jsonrpcRequest, transportContext, jsonrpcResponse,
            rsp -> new HttpJsonRpcResponse(status(rsp), rsp));
    }

    private Mono<HttpJsonRpcResponse> handleJsonRPCResponse(McpSchema.JSONRPCMessage jsonrpcMessage,
                                                        McpTransportContext transportContext,
                                                        Mono<McpSchema.JSONRPCResponse> jsonrpcResponse,
                                                        Function<McpSchema.JSONRPCResponse, HttpJsonRpcResponse> jsonrpcResponseHttpResponseFunction) {
        return jsonrpcResponse.contextWrite(ctx -> ctx.put(McpTransportContext.KEY, transportContext))
            .onErrorResume(McpError.class, e -> {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Failed to handle JSON RPC Message: {}", e.getMessage());
                }
                return Mono.just(errorJsonrpcResponse(jsonrpcMessage, e));
            })
            .onErrorResume(throwable -> {
                if (LOG.isErrorEnabled()) {
                    LOG.error("Failed to handle JSON RPC Message: {}", throwable.getMessage());
                }
                return Mono.just(errorJsonrpcResponse(jsonrpcMessage,
                    mcpError(McpSchema.ErrorCodes.INTERNAL_ERROR, "Failed to handle request: " + throwable.getMessage())));
            }).map(jsonrpcResponseHttpResponseFunction::apply);
    }

    @Nullable
    private McpSchema.JSONRPCMessage jsonRpcMessage(@NonNull Map<String, Object> body) {
        if (body.containsKey(KEY_METHOD) && body.containsKey(KEY_ID)) {
            return new McpSchema.JSONRPCRequest(body.get(KEY_JSONRPC).toString(), body.get(KEY_METHOD).toString(), body.get(KEY_ID), body.get(KEY_PARAMS));
        } else if (body.containsKey(KEY_METHOD) && !body.containsKey(KEY_ID)) {
            return new McpSchema.JSONRPCNotification(body.get(KEY_JSONRPC).toString(), body.get(KEY_METHOD).toString(), body.get(KEY_PARAMS));
        }
        return null;
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

    @NonNull
    private static int status(@Nullable McpSchema.JSONRPCResponse response) {
        if (response == null || response.error() == null) {
            return 200;
        }
        return status(response.error());
    }

    @NonNull
    static int status(@NonNull McpSchema.JSONRPCResponse.JSONRPCError error) {
        if (error.code() == McpSchema.ErrorCodes.PARSE_ERROR) {
            return 400;
        } else if (error.code() == McpSchema.ErrorCodes.INVALID_REQUEST) {
            return 400;
        } else if (error.code() == McpSchema.ErrorCodes.METHOD_NOT_FOUND) {
            return 400;
        } else if (error.code() == McpSchema.ErrorCodes.INVALID_PARAMS) {
            return 400;
        } else if (error.code() == McpSchema.ErrorCodes.INTERNAL_ERROR) {
            return 500;
        }
        return 500;
    }

    @NonNull
    private static McpError mcpError(int error, String message) {
        return McpError.builder(error)
            .message(message)
            .build();
    }
}
