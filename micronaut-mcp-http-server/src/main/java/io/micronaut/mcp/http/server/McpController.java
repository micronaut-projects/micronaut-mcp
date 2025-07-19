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
package io.micronaut.mcp.http.server;

import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.util.StringUtils;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.RequestBean;
import io.micronaut.http.annotation.Status;
import io.micronaut.mcp.ErrorMcpResponse;
import io.micronaut.mcp.McpRequest;
import io.micronaut.mcp.McpRequestHandler;
import io.micronaut.mcp.McpResponse;
import io.micronaut.mcp.SuccessfulMcpResponse;
import io.micronaut.mcp.jsonrpc.Request;
import jakarta.validation.Valid;

@Requires(property = McpControllerConfiguration.PROPERTY_ENABLED, notEquals = StringUtils.FALSE, defaultValue = StringUtils.TRUE)
@Controller("${" + McpControllerConfiguration.PROPERTY_PATH + ":" + McpControllerConfiguration.DEFAULT_PATH + "}")
@Internal
class McpController {
    private static final String MCP_METHOD_PING = "ping";
    private final McpRequestHandler mcpRequestHandler;

    McpController(McpRequestHandler mcpRequestHandler) {
        this.mcpRequestHandler = mcpRequestHandler;
    }

    @Post
    HttpResponse<?> mcpPost(@NonNull @RequestBean McpHttpRequest mcpHttpRequest,
                            @NonNull @Body @Valid Request<?, ?> jsonRpcRequest) {
        McpRequest mcpRequest = new McpRequest(mcpHttpRequest.protocolVersion(), mcpHttpRequest.sessionId(), mcpHttpRequest.lastEventId(), jsonRpcRequest);
        McpResponse mcpResponse = mcpRequestHandler.handle(mcpRequest);
        if (mcpResponse instanceof SuccessfulMcpResponse rsp) {
            return HttpResponse.ok(rsp.response());
        }
        if (mcpResponse instanceof ErrorMcpResponse rsp) {
            return HttpResponse.unprocessableEntity().body(rsp.response());
        }
        return HttpResponse.unprocessableEntity();
    }

    @Get
    @Status(HttpStatus.OK)
    void mcpGet(@NonNull @RequestBean McpHttpRequest mcpHttpRequest) {
        //TODO will implement in future PRs
    }
}
