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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.core.annotation.Internal;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Produces;
import io.micronaut.mcp.server.conf.McpServerConfiguration;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.modelcontextprotocol.server.DefaultMcpTransportContext;
import io.modelcontextprotocol.server.McpStatelessServerHandler;
import io.modelcontextprotocol.server.McpTransportContext;
import io.modelcontextprotocol.server.McpTransportContextExtractor;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.io.IOException;

@Controller("${" + McpServerConfiguration.PROPERTY_ENDPOINT + ":" + McpServerConfiguration.DEFAULT_ENDPOINT + "}")
@Internal
final class McpController {
    private static final Logger LOG = LoggerFactory.getLogger(McpController.class);

    private final ObjectMapper objectMapper;
    private final McpStatelessServerHandler mcpHandler;
    private final McpTransportContextExtractor<HttpRequest<?>> contextExtractor;

    McpController(ObjectMapper objectMapper,
                  McpStatelessServerHandler mcpHandler,
                  McpTransportContextExtractor<HttpRequest<?>> contextExtractor) {
        this.objectMapper = objectMapper;
        this.mcpHandler = mcpHandler;
        this.contextExtractor = contextExtractor;
    }

    @SuppressWarnings("java:S3740")
    @ExecuteOn(TaskExecutors.IO)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM})
    @Post
    public Mono<HttpResponse<?>> handlePost(HttpRequest<?> request, @Body String body) {
        McpTransportContext transportContext = contextExtractor.extract(request, new DefaultMcpTransportContext());
        try {
            McpSchema.JSONRPCMessage message = McpSchema.deserializeJsonRpcMessage(objectMapper, body);
            if (message instanceof McpSchema.JSONRPCRequest jsonrpcRequest) {
                return handleJsonRpcRequest(jsonrpcRequest, transportContext);
            } else if (message instanceof McpSchema.JSONRPCNotification jsonrpcNotification) {
                return handleJsonRpcNotification(jsonrpcNotification, transportContext);
            } else {
                return Mono.just(HttpResponse.badRequest(mcpError(McpSchema.ErrorCodes.INVALID_REQUEST, "The server accepts either requests or notifications")));
            }
        } catch (IllegalArgumentException | IOException e) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Failed to deserialize message: {}", e.getMessage());
            }
            return Mono.just(HttpResponse.badRequest(mcpError(McpSchema.ErrorCodes.PARSE_ERROR, "Invalid message format")));
        } catch (Exception e) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Unexpected error handling message: {}", e.getMessage());
            }
            return Mono.just(
                HttpResponse.serverError(mcpError(McpSchema.ErrorCodes.INTERNAL_ERROR, "Unexpected error: " + e.getMessage()))
            );
        }
    }

    @SuppressWarnings("java:S3740")
    private Mono<HttpResponse<?>> handleJsonRpcNotification(McpSchema.JSONRPCNotification jsonrpcNotification, McpTransportContext transportContext) {
        try {
            return mcpHandler.handleNotification(transportContext, jsonrpcNotification)
                .contextWrite(ctx -> ctx.put(McpTransportContext.KEY, transportContext))
                .thenReturn(HttpResponse.accepted());
        } catch (Exception e) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Failed to handle notification: {}", e.getMessage());
            }
            return Mono.just(
                HttpResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(mcpError(McpSchema.ErrorCodes.INTERNAL_ERROR, "Failed to handle notification: " + e.getMessage()))
            );
        }
    }

    @SuppressWarnings("java:S3740")
    private Mono<HttpResponse<?>> handleJsonRpcRequest(McpSchema.JSONRPCRequest jsonrpcRequest, McpTransportContext transportContext) {
        try {
            Mono<McpSchema.JSONRPCResponse> jsonrpcResponse = mcpHandler.handleRequest(transportContext, jsonrpcRequest)
                .contextWrite(ctx -> ctx.put(McpTransportContext.KEY, transportContext));
            return jsonrpcResponse.map(HttpResponse::ok);
        } catch (Exception e) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Failed to handle request: {}", e.getMessage());
            }
            return Mono.just(
                HttpResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(mcpError(McpSchema.ErrorCodes.INTERNAL_ERROR, "Failed to handle request: " + e.getMessage()))
            );
        }
    }

    private static McpError mcpError(int error, String message) {
        return McpError.builder(error)
            .message(message)
            .build();
    }
}
