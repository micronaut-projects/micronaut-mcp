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
import io.micronaut.json.JsonMapper;
import io.micronaut.json.tree.JsonNode;
import io.micronaut.mcp.conf.server.McpServerConfiguration;
import io.micronaut.mcp.server.context.McpNotificationEmitter;
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
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Handles the POST endpoint, in route {@value McpServerConfiguration#DEFAULT_ENDPOINT}.
 * The route can be configured via the property {@value McpServerConfiguration#PROPERTY_ENDPOINT}.
 * The endpoint expects to receive JSON RPC Messages, and it responds JSON RPC Messages.
 * @see <a href="https://modelcontextprotocol.io/specification/2025-06-18/basic/transports#streamable-http">Streamble HTTP Transport</a>.
 */
@Internal
abstract sealed class McpController permits McpReactiveController, McpBlockingController {
    static final String PATH = "${" + McpServerConfiguration.PROPERTY_ENDPOINT + ":" + McpServerConfiguration.DEFAULT_ENDPOINT + "}";

    private static final Logger LOG = LoggerFactory.getLogger(McpController.class);
    private static final String KEY_METHOD = "method";
    private static final String KEY_ID = "id";
    private static final String KEY_JSONRPC = "jsonrpc";
    private static final String KEY_PARAMS = "params";
    private static final String KEY_LEVEL = "level";
    // The levels as the protocol spells them: the lower-case names of the constants
    private static final Set<String> LOGGING_LEVELS = Arrays.stream(McpSchema.LoggingLevel.values())
        .map(level -> level.name().toLowerCase(Locale.ROOT))
        .collect(Collectors.toCollection(LinkedHashSet::new));
    private static final String INVALID_BATCH = "Invalid Request: batches are not supported, send one JSON-RPC message per request";
    private static final String INVALID_BODY = "Invalid Request: the body must be a JSON object";
    private static final String INVALID_JSONRPC = "Invalid Request: \"jsonrpc\" must be \"2.0\"";
    private static final String INVALID_METHOD = "Invalid Request: \"method\" must be a string";
    private static final String INVALID_ID = "Invalid Request: \"id\" must be a string or an integer";
    private static final String INVALID_PARAMS = "Invalid Request: \"params\" must be an object or an array";
    private static final String UNREADABLE_PARAMS = "Invalid Request: \"params\" could not be read";
    private static final String INTERNAL_ERROR = "Internal error";
    private static final int MAX_LOGGED_METHOD_LENGTH = 64;

    private final McpStatelessServerHandler mcpHandler;
    private final McpTransportContextExtractor<HttpRequest<?>> contextExtractor;
    private final JsonMapper jsonMapper;
    private final boolean passJsonTrees;
    private final boolean logging;

    McpController(McpStatelessServerHandler mcpHandler,
                  McpTransportContextExtractor<HttpRequest<?>> contextExtractor,
                  JsonMapper jsonMapper,
                  McpJsonMapper mcpJsonMapper,
                  McpSchema.ServerCapabilities capabilities) {
        this.mcpHandler = mcpHandler;
        this.contextExtractor = contextExtractor;
        this.jsonMapper = jsonMapper;
        // The Micronaut MCP JSON mapper converts the parsed parameters directly; any other mapper gets them as a Map
        this.passJsonTrees = mcpJsonMapper instanceof MicronautMcpJsonMapper;
        this.logging = capabilities.logging() != null;
    }

    /**
     * @param request The request
     * @param body The body of the request
     * @param streaming Whether the response may become an event stream of the notifications of the request, as {@link McpStreamingPolicy} decides
     * @return The response
     */
    @SuppressWarnings("java:S3740")
    final Mono<HttpResponse<?>> handle(HttpRequest<?> request, @Nullable JsonNode body, boolean streaming) {
        McpSchema.JSONRPCMessage jsonRpcMessage;
        try {
            jsonRpcMessage = jsonRpcMessage(body);
        } catch (InvalidRequestException e) {
            return Mono.just(HttpResponse.badRequest(invalidRequest(e.getMessage())));
        }
        McpTransportContext transportContext = contextExtractor.extract(request);
        if (jsonRpcMessage instanceof McpSchema.JSONRPCRequest jsonrpcRequest) {
            if (logging && McpSchema.METHOD_LOGGING_SET_LEVEL.equals(jsonrpcRequest.method())) {
                // The SDK has no handler for it on stateless servers, which cannot remember a level between requests
                return Mono.just(setLevel(jsonrpcRequest, body));
            }
            if (streaming) {
                return Mono.create(sink -> {
                    StreamingJsonRpcResponse response = new StreamingJsonRpcResponse(sink, jsonrpcRequest);
                    request.setAttribute(McpNotificationEmitter.ATTRIBUTE, response);
                    response.subscription(handleJsonRpcRequest(jsonrpcRequest, transportContext)
                        // The Reactor context of the route, with the propagated context, reaches the primitive
                        .contextWrite(sink.contextView())
                        .subscribe(response::complete, response::fail, response::completeEmpty));
                });
            }
            return handleJsonRpcRequest(jsonrpcRequest, transportContext);
        }
        return handleJsonRpcNotification((McpSchema.JSONRPCNotification) jsonRpcMessage, transportContext);
    }

    /**
     * Acknowledges a {@code logging/setLevel} request with a valid level. A stateless server cannot remember the level,
     * so it sends every log message whatever its level.
     */
    @SuppressWarnings("java:S1452")
    private static HttpResponse<?> setLevel(McpSchema.JSONRPCRequest jsonrpcRequest, @Nullable JsonNode body) {
        JsonNode params = body != null ? body.get(KEY_PARAMS) : null;
        JsonNode level = params != null && params.isObject() ? params.get(KEY_LEVEL) : null;
        if (level == null || !level.isString() || !LOGGING_LEVELS.contains(level.getStringValue())) {
            McpSchema.JSONRPCResponse rsp = JsonRrpcResponseUtils.jsonrpcResponse(
                mcpError(McpSchema.ErrorCodes.INVALID_PARAMS, "Invalid logging level, expected one of " + LOGGING_LEVELS), jsonrpcRequest);
            return HttpResponse.status(status(rsp)).body(rsp);
        }
        return HttpResponse.ok(new McpSchema.JSONRPCResponse(McpSchema.JSONRPC_VERSION, jsonrpcRequest.id(), Map.of(), null));
    }

    @SuppressWarnings("java:S3740")
    private Mono<HttpResponse<?>> handleJsonRpcNotification(McpSchema.JSONRPCNotification jsonrpcNotification, McpTransportContext transportContext) {
        // deferred so that a handler that throws is answered like one that emits an error
        return Mono.defer(() -> mcpHandler.handleNotification(transportContext, jsonrpcNotification))
            .contextWrite(ctx -> ctx.put(McpTransportContext.KEY, transportContext))
            .then(Mono.<HttpResponse<?>>fromSupplier(HttpResponse::accepted))
            .onErrorResume(e -> notificationFailure(e, jsonrpcNotification));
    }

    // A notification has no id, so no JSON-RPC response can carry its error: the HTTP status alone reports it
    private Mono<HttpResponse<?>> notificationFailure(Throwable e, McpSchema.JSONRPCNotification jsonrpcNotification) {
        if (e instanceof McpError mcpError) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Failed to handle JSON-RPC notification {}: {}", loggable(jsonrpcNotification.method()), mcpError.getMessage());
            }
            McpSchema.JSONRPCResponse.JSONRPCError error = mcpError.getJsonRpcError();
            return Mono.just(HttpResponse.status(error != null ? status(error) : HttpStatus.INTERNAL_SERVER_ERROR));
        }
        if (LOG.isErrorEnabled()) {
            LOG.error("Failed to handle JSON-RPC notification {}", loggable(jsonrpcNotification.method()), e);
        }
        return Mono.just(HttpResponse.serverError());
    }

    private Mono<HttpResponse<?>> internalErrorResponse(Throwable e, McpSchema.JSONRPCRequest jsonrpcRequest) {
        // an exception other than an McpError is a server fault: log it, and do not show its message to the client
        if (LOG.isErrorEnabled()) {
            LOG.error("Failed to handle JSON-RPC request {}", loggable(jsonrpcRequest.method()), e);
        }
        McpError mcpError = mcpError(McpSchema.ErrorCodes.INTERNAL_ERROR, INTERNAL_ERROR);
        return exceptionJsonRpcResponse(mcpError, jsonrpcRequest);
    }

    private Mono<HttpResponse<?>> exceptionJsonRpcResponse(McpError e, McpSchema.JSONRPCRequest jsonrpcMessage) {
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
        // deferred so that a handler that throws instead of returning an error reaches the JSON-RPC error mapping
        Mono<HttpResponse<?>> mono = Mono.defer(() -> mcpHandler.handleRequest(transportContext, jsonrpcRequest))
            .contextWrite(ctx -> ctx.put(McpTransportContext.KEY, transportContext))
            .map(rsp -> HttpResponse.status(status(rsp)).body(rsp));
        return mono
            .onErrorResume(McpError.class, e -> exceptionJsonRpcResponse(e, jsonrpcRequest))
            .onErrorResume(throwable -> internalErrorResponse(throwable, jsonrpcRequest));
    }

    private McpSchema.JSONRPCMessage jsonRpcMessage(@Nullable JsonNode body) throws InvalidRequestException {
        if (body == null || !body.isObject()) {
            throw new InvalidRequestException(body != null && body.isArray() ? INVALID_BATCH : INVALID_BODY);
        }
        JsonNode jsonrpc = body.get(KEY_JSONRPC);
        if (jsonrpc == null || !jsonrpc.isString() || !McpSchema.JSONRPC_VERSION.equals(jsonrpc.getStringValue())) {
            throw new InvalidRequestException(INVALID_JSONRPC);
        }
        JsonNode method = body.get(KEY_METHOD);
        if (method == null || !method.isString()) {
            throw new InvalidRequestException(INVALID_METHOD);
        }
        Object params = params(body.get(KEY_PARAMS));
        JsonNode id = body.get(KEY_ID);
        if (id == null) {
            return new McpSchema.JSONRPCNotification(jsonrpc.getStringValue(), method.getStringValue(), params);
        }
        Object requestId = id(id);
        if (requestId == null) {
            throw new InvalidRequestException(INVALID_ID);
        }
        return new McpSchema.JSONRPCRequest(jsonrpc.getStringValue(), method.getStringValue(), requestId, params);
    }

    private @Nullable Object params(@Nullable JsonNode params) throws InvalidRequestException {
        if (params == null || params.isNull()) {
            return null;
        }
        if (!params.isObject() && !params.isArray()) {
            // JSON-RPC parameters are structured values
            throw new InvalidRequestException(INVALID_PARAMS);
        }
        if (passJsonTrees) {
            return params;
        }
        try {
            return jsonMapper.readValueFromTree(params, Object.class);
        } catch (IOException _) {
            throw new InvalidRequestException(UNREADABLE_PARAMS);
        }
    }

    private static @Nullable Object id(JsonNode id) {
        if (id.isString()) {
            return id.getStringValue();
        }
        if (id.isNumber()) {
            Number number = id.getNumberValue();
            if (number instanceof Integer || number instanceof Long) {
                return number;
            }
        }
        // MCP request ids are strings or integers
        return null;
    }

    // The method is client input: only a short printable name is written to the log
    private static String loggable(String method) {
        String name = method.length() > MAX_LOGGED_METHOD_LENGTH ? method.substring(0, MAX_LOGGED_METHOD_LENGTH) : method;
        return name.chars().allMatch(c -> c > ' ' && c < 0x7f) ? name : "<non-printable method>";
    }

    // JSON-RPC answers a message that is not a valid request with a null id, which the SDK response type does not allow
    private static Map<String, Object> invalidRequest(String message) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put(KEY_JSONRPC, McpSchema.JSONRPC_VERSION);
        response.put(KEY_ID, null);
        response.put("error", Map.of("code", McpSchema.ErrorCodes.INVALID_REQUEST, "message", message));
        return response;
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

    /**
     * A message that is not a valid JSON-RPC request or notification. It has no stack trace: it is answered, not logged.
     */
    private static final class InvalidRequestException extends Exception {
        InvalidRequestException(String message) {
            super(message, null, false, false);
        }
    }
}
