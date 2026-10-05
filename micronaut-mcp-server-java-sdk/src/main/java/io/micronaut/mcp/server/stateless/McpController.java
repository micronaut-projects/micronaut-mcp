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
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.json.JsonMapper;
import io.micronaut.json.tree.JsonNode;
import io.micronaut.mcp.conf.server.McpServerConfiguration;
import io.micronaut.mcp.server.exceptions.JsonRrpcResponseUtils;
import io.micronaut.mcp.server.json.MicronautMcpJsonMapper;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpStatelessServerHandler;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.server.McpTransportContextExtractor;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

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
    // JSON-RPC answers a message that is not a valid request with a null id, which the SDK response type does not allow
    private static final Map<String, Object> INVALID_REQUEST = invalidRequest();

    private final McpStatelessServerHandler mcpHandler;
    private final McpTransportContextExtractor<HttpRequest<?>> contextExtractor;
    private final JsonMapper jsonMapper;
    private final boolean passJsonTrees;

    McpController(McpStatelessServerHandler mcpHandler,
                  McpTransportContextExtractor<HttpRequest<?>> contextExtractor,
                  JsonMapper jsonMapper,
                  McpJsonMapper mcpJsonMapper) {
        this.mcpHandler = mcpHandler;
        this.contextExtractor = contextExtractor;
        this.jsonMapper = jsonMapper;
        // The Micronaut MCP JSON mapper converts the parsed parameters directly; any other mapper gets them as a Map
        this.passJsonTrees = mcpJsonMapper instanceof MicronautMcpJsonMapper;
    }

    @SuppressWarnings("java:S3740")
    @Post
    public Mono<HttpResponse<?>> handlePost(HttpRequest<?> request, @Body JsonNode body) {
        McpTransportContext transportContext = contextExtractor.extract(request);
        McpSchema.JSONRPCMessage jsonRpcMessage = jsonRpcMessage(body);
        if (jsonRpcMessage instanceof McpSchema.JSONRPCRequest jsonrpcRequest) {
            return handleJsonRpcRequest(jsonrpcRequest, transportContext);
        } else if (jsonRpcMessage instanceof McpSchema.JSONRPCNotification notification) {
            return handleJsonRpcNotification(notification, transportContext);
        }
        return Mono.just(HttpResponse.badRequest(INVALID_REQUEST));
    }

    @SuppressWarnings("java:S3740")
    private Mono<HttpResponse<?>> handleJsonRpcNotification(McpSchema.JSONRPCNotification jsonrpcNotification, McpTransportContext transportContext) {
        return mcpHandler.handleNotification(transportContext, jsonrpcNotification)
            .map((Function<Void, HttpResponse<?>>) response -> HttpResponse.accepted())
            .defaultIfEmpty(HttpResponse.accepted())
            .contextWrite(ctx -> ctx.put(McpTransportContext.KEY, transportContext))
            .onErrorResume(McpError.class, e -> exceptionJsonRpcResponse(e, jsonrpcNotification))
            .onErrorResume(throwable -> exceptionJsonRpcResponse(throwable, jsonrpcNotification));
    }

    private Mono<HttpResponse<?>> exceptionJsonRpcResponse(Throwable e, McpSchema.JSONRPCMessage jsonrpcMessage) {
        McpError mcpError = mcpError(McpSchema.ErrorCodes.INTERNAL_ERROR, "Failed to handle request: " + e.getMessage());
        return exceptionJsonRpcResponse(mcpError, jsonrpcMessage);
    }

    private Mono<HttpResponse<?>> exceptionJsonRpcResponse(McpError e, McpSchema.JSONRPCMessage jsonrpcMessage) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Failed to handle JSON RPC Message: {}", e.getMessage());
        }
        McpSchema.JSONRPCResponse rsp = JsonRrpcResponseUtils.jsonrpcResponse(e, jsonrpcMessage);
        return Mono.just(HttpResponse.status(status(rsp)).body(rsp));
    }

    @SuppressWarnings("java:S3740")
    @NonNull
    private Mono<HttpResponse<?>> handleJsonRpcRequest(McpSchema.@NonNull JSONRPCRequest jsonrpcRequest,
                                                       @NonNull McpTransportContext transportContext) {
        Mono<HttpResponse<?>> mono = mcpHandler.handleRequest(transportContext, jsonrpcRequest)
            .contextWrite(ctx -> ctx.put(McpTransportContext.KEY, transportContext))
            .map(rsp -> HttpResponse.status(status(rsp)).body(rsp));
        return mono
            .onErrorResume(McpError.class, e -> exceptionJsonRpcResponse(e, jsonrpcRequest))
            .onErrorResume(throwable -> exceptionJsonRpcResponse(throwable, jsonrpcRequest));
    }

    private McpSchema.@Nullable JSONRPCMessage jsonRpcMessage(@Nullable JsonNode body) {
        if (body == null || !body.isObject()) {
            return null;
        }
        JsonNode jsonrpc = body.get(KEY_JSONRPC);
        JsonNode method = body.get(KEY_METHOD);
        if (jsonrpc == null || !jsonrpc.isString() || method == null || !method.isString()) {
            return null;
        }
        Object params = params(body.get(KEY_PARAMS));
        JsonNode id = body.get(KEY_ID);
        if (id == null) {
            return new McpSchema.JSONRPCNotification(jsonrpc.getStringValue(), method.getStringValue(), params);
        }
        return new McpSchema.JSONRPCRequest(jsonrpc.getStringValue(), method.getStringValue(), id(id), params);
    }

    private @Nullable Object params(@Nullable JsonNode params) {
        if (params == null || params.isNull()) {
            return null;
        }
        if (passJsonTrees) {
            return params;
        }
        try {
            return jsonMapper.readValueFromTree(params, Object.class);
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid JSON-RPC params", e);
        }
    }

    private static @Nullable Object id(JsonNode id) {
        if (id.isString()) {
            return id.getStringValue();
        }
        if (id.isNumber()) {
            return id.getNumberValue();
        }
        return id.isNull() ? null : id.getValue();
    }

    private static Map<String, Object> invalidRequest() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put(KEY_JSONRPC, McpSchema.JSONRPC_VERSION);
        response.put(KEY_ID, null);
        response.put("error", Map.of(
            "code", McpSchema.ErrorCodes.INVALID_REQUEST,
            "message", "The server accepts either JSON-RPC requests or notifications"));
        return Collections.unmodifiableMap(response);
    }

    @NonNull
    private static HttpStatus status(McpSchema.@Nullable JSONRPCResponse response) {
        if (response == null || response.error() == null) {
            return HttpStatus.OK;
        }
        return status(response.error());
    }

    @NonNull
    static HttpStatus status(McpSchema.JSONRPCResponse.@NonNull JSONRPCError error) {
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
