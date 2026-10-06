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

import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.util.StringUtils;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.json.JsonMapper;
import io.micronaut.json.tree.JsonNode;
import io.micronaut.mcp.conf.server.McpServerConfiguration;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpStatelessServerHandler;
import io.modelcontextprotocol.server.McpTransportContextExtractor;
import org.jspecify.annotations.Nullable;

/**
 * The MCP endpoint of a synchronous server. Requests are handled on the blocking executor, which runs on virtual threads
 * where they are available, and the server invokes the primitives on that same thread, so the request context propagates
 * to them and they may block.
 */
@Controller(McpController.PATH)
@Requires(property = McpServerConfiguration.PROPERTY_REACTIVE, value = StringUtils.FALSE, defaultValue = StringUtils.FALSE)
@ExecuteOn(TaskExecutors.BLOCKING)
@Internal
final class McpBlockingController extends McpController {

    McpBlockingController(McpStatelessServerHandler mcpHandler,
                          McpTransportContextExtractor<HttpRequest<?>> contextExtractor,
                          JsonMapper jsonMapper,
                          McpJsonMapper mcpJsonMapper,
                          McpRequestValidator requestValidator) {
        super(mcpHandler, contextExtractor, jsonMapper, mcpJsonMapper, requestValidator);
    }

    // The route returns a JSON-RPC response or an error body, so the response body type is a wildcard
    @SuppressWarnings({"java:S3740", "java:S1452"})
    @Post
    @Nullable HttpResponse<?> handlePost(HttpRequest<?> request, @Body @Nullable JsonNode body) {
        return handle(request, body).block();
    }
}
