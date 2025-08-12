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
package io.micronaut.mcp.server.sdk.stateless;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.core.annotation.Internal;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.util.HttpHeadersUtil;
import io.micronaut.mcp.server.sdk.conf.McpServerConfiguration;
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
import java.io.IOException;

@Controller("${" + McpServerConfiguration.PROPERTY_ENDPOINT + ":" + McpServerConfiguration.DEFAULT_ENDPOINT + "}")
@Internal
class McpController {
    private final Logger LOG = LoggerFactory.getLogger(McpController.class);

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

    @ExecuteOn(TaskExecutors.IO)
    @Produces({MediaType.APPLICATION_JSON,  MediaType.TEXT_EVENT_STREAM})
    @Post
    public MutableHttpResponse<?> handlePost(HttpRequest<?> request, @Body String body) {
        if (LOG.isTraceEnabled()) {
            HttpHeadersUtil.trace(LOG, request.getHeaders());
            LOG.info("Received JSON RPC Message: {}", body);
        }

        McpTransportContext transportContext = contextExtractor.extract(request, new DefaultMcpTransportContext());
        try {
            McpSchema.JSONRPCMessage message = McpSchema.deserializeJsonRpcMessage(objectMapper, body);
            if (message instanceof McpSchema.JSONRPCRequest jsonrpcRequest) {
                try {
                    McpSchema.JSONRPCResponse jsonrpcResponse = this.mcpHandler
                        .handleRequest(transportContext, jsonrpcRequest)
                        .contextWrite(ctx -> ctx.put(McpTransportContext.KEY, transportContext))
                        .block();
                    return HttpResponse.ok().contentType(MediaType.APPLICATION_JSON).body(jsonrpcResponse);
                } catch (Exception e) {
                    if (LOG.isErrorEnabled()) {
                        LOG.error("Failed to handle request: {}", e.getMessage());
                    }
                    return HttpResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new McpError("Failed to handle request: " + e.getMessage()));
                }
            } else if (message instanceof McpSchema.JSONRPCNotification jsonrpcNotification) {
                try {
                    this.mcpHandler.handleNotification(transportContext, jsonrpcNotification)
                        .contextWrite(ctx -> ctx.put(McpTransportContext.KEY, transportContext))
                        .block();
                    return HttpResponse.accepted();
                } catch (Exception e) {
                    LOG.error("Failed to handle notification: {}", e.getMessage());
                    return HttpResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new McpError("Failed to handle notification: " + e.getMessage()));
                }
            } else {
                return HttpResponse.badRequest()
                    .body(new McpError("The server accepts either requests or notifications"));
            }
        } catch (IllegalArgumentException | IOException e) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Failed to deserialize message: {}", e.getMessage());
            }
            return HttpResponse.badRequest().body(new McpError("Invalid message format"));
        } catch (Exception e) {
            LOG.error("Unexpected error handling message: {}", e.getMessage());
            return HttpResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new McpError("Unexpected error: " + e.getMessage()));
        }
    }
}
