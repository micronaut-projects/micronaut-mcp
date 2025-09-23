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
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Produces;
import io.micronaut.mcp.conf.McpServerConfiguration;
import io.micronaut.mcp.server.stateless.transport.HttpJsonRpcResponse;
import io.micronaut.mcp.server.stateless.transport.HttpServerMcpStatelessServerTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import java.util.Map;

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

    private final HttpServerMcpStatelessServerTransport<HttpRequest<?>> transport;

    McpController(HttpServerMcpStatelessServerTransport<HttpRequest<?>> transport) {
        this.transport = transport;
    }

    @SuppressWarnings("java:S3740")
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM})
    @Post
    public Mono<HttpResponse<?>> handlePost(HttpRequest<?> request, @Body Map<String, Object> body) {
        return map(transport.handlePost(request, body));
    }

    static Mono<HttpResponse<?>> map(Mono<HttpJsonRpcResponse> httpJsonRpcResponseMono) {
        return httpJsonRpcResponseMono.map(rsp -> {
            MutableHttpResponse<?> response = HttpResponse.status(rsp.statusCode(), rsp.statusReason());
            if (rsp.body() != null) {
                response.body(rsp.body());
            }
            return response;
        });
    }
}
