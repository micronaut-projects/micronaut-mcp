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
package io.micronaut.mcp.server.stateless;

import io.micronaut.core.annotation.Internal;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Produces;
import io.micronaut.mcp.conf.McpServerConfiguration;
import io.modelcontextprotocol.server.McpStatelessServerHandler;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.server.McpTransportContextExtractor;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import java.util.Map;
import java.util.function.Function;

import static io.modelcontextprotocol.spec.McpSchema.JSONRPC_VERSION;

/**
 * This class exposes a POST endpoint in route {@value McpServerConfiguration#DEFAULT_ENDPOINT}.
 * The route can be configured via the property {@value McpServerConfiguration#PROPERTY_ENDPOINT}.
 * The endpoint expects to receive JSON RPC Messages, and it responds JSON RPC Messages.
 * @see <a href="https://modelcontextprotocol.io/specification/2025-06-18/basic/transports#streamable-http">Streamble HTTP Transport</a>.
 */
@Controller("${" + McpServerConfiguration.PROPERTY_ENDPOINT + ":" + McpServerConfiguration.DEFAULT_ENDPOINT + "}")
@Internal
final class McpController {
    private static final Logger LOG = LoggerFactory.getLogger(McpController.class);
    private static final String KEY_METHOD = "method";
    private static final String KEY_ID = "id";
    private static final String KEY_JSONRPC = "jsonrpc";
    private static final String KEY_PARAMS = "params";

    private final McpStatelessServerHandler mcpHandler;
    private final McpTransportContextExtractor<HttpRequest<?>> contextExtractor;

    McpController(McpStatelessServerHandler mcpHandler,
                  McpTransportContextExtractor<HttpRequest<?>> contextExtractor) {
        this.mcpHandler = mcpHandler;
        this.contextExtractor = contextExtractor;
    }

    @SuppressWarnings("java:S3740")
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM})
    @Post
    public Mono<HttpResponse<?>> handlePost(HttpRequest<?> request, @Body Map<String, Object> body) {
        McpTransportContext transportContext = contextExtractor.extract(request);
        McpSchema.JSONRPCMessage jsonRpcMessage = jsonRpcMessage(body);
        if (jsonRpcMessage instanceof McpSchema.JSONRPCRequest jsonrpcRequest) {
            return handleJsonRpcRequest(jsonrpcRequest, transportContext);
        } else if (jsonRpcMessage instanceof McpSchema.JSONRPCNotification notification) {
            return handleJsonRpcNotification(notification, transportContext);
        }
        return Mono.just(HttpResponse.badRequest(mcpError(McpSchema.ErrorCodes.INVALID_REQUEST, "The server accepts either requests or notifications")));
    }

    @SuppressWarnings("java:S3740")
    private Mono<HttpResponse<?>> handleJsonRpcNotification(McpSchema.JSONRPCNotification jsonrpcNotification, McpTransportContext transportContext) {

        Mono<McpSchema.JSONRPCResponse> jsonrpcResponse = mcpHandler.handleNotification(transportContext, jsonrpcNotification)
            .then(Mono.fromCallable(() -> {
                McpSchema.JSONRPCResponse response = null;
                return response;
            }));
        return handleJsonRPCResponse(jsonrpcNotification, transportContext, jsonrpcResponse, rsp -> {
            HttpStatus status = status(rsp);
            if (status.getCode() >= 400) {
                return HttpResponse.status(status).body(rsp);
            }
            return HttpResponse.accepted();
        });
    }

    @SuppressWarnings("java:S3740")
    @NonNull
    private Mono<HttpResponse<?>> handleJsonRpcRequest(@NonNull McpSchema.JSONRPCRequest jsonrpcRequest,
                                                       @NonNull McpTransportContext transportContext) {
        Mono<McpSchema.JSONRPCResponse> jsonrpcResponse = mcpHandler.handleRequest(transportContext, jsonrpcRequest);
        return handleJsonRPCResponse(jsonrpcRequest, transportContext, jsonrpcResponse,
            rsp -> HttpResponse.status(status(rsp)).body(rsp));
    }

    private Mono<HttpResponse<?>> handleJsonRPCResponse(McpSchema.JSONRPCMessage jsonrpcMessage,
                                                        McpTransportContext transportContext,
                                                        Mono<McpSchema.JSONRPCResponse> jsonrpcResponse,
                                                        Function<McpSchema.JSONRPCResponse, HttpResponse> jsonrpcResponseHttpResponseFunction) {
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
    private static HttpStatus status(@Nullable McpSchema.JSONRPCResponse response) {
        if (response == null || response.error() == null) {
            return HttpStatus.OK;
        }
        return status(response.error());
    }

    @NonNull
    static HttpStatus status(@NonNull McpSchema.JSONRPCResponse.JSONRPCError error) {
        if (error.code() == McpSchema.ErrorCodes.PARSE_ERROR) {
            return HttpStatus.BAD_REQUEST;
        } else if (error.code() == McpSchema.ErrorCodes.INVALID_REQUEST) {
            return HttpStatus.BAD_REQUEST;
        } else if (error.code() == McpSchema.ErrorCodes.METHOD_NOT_FOUND) {
            return HttpStatus.BAD_REQUEST;
        } else if (error.code() == McpSchema.ErrorCodes.INVALID_PARAMS) {
            return HttpStatus.BAD_REQUEST;
        } else if (error.code() == McpSchema.ErrorCodes.INTERNAL_ERROR) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    @NonNull
    private static McpError mcpError(int error, String message) {
        return McpError.builder(error)
            .message(message)
            .build();
    }
}
