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
import io.micronaut.mcp.server.registry.PromptRegistry;
import io.micronaut.mcp.server.registry.ResourceRegistry;
import io.micronaut.mcp.server.registry.ToolRegistry;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Named;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.ExecutorService;
import java.util.function.Predicate;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpStatelessServerHandler;
import io.modelcontextprotocol.server.McpTransportContextExtractor;
import org.jspecify.annotations.Nullable;
import reactor.core.publisher.Mono;

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
    private final ToolRegistry toolRegistry;
    private final PromptRegistry promptRegistry;
    private final ResourceRegistry resourceRegistry;
    private final Scheduler blockingScheduler;

    McpBlockingController(McpStatelessServerHandler mcpHandler,
                          McpTransportContextExtractor<HttpRequest<?>> contextExtractor,
                          JsonMapper jsonMapper,
                          McpJsonMapper mcpJsonMapper,
                          ToolRegistry toolRegistry,
                          PromptRegistry promptRegistry,
                          ResourceRegistry resourceRegistry,
                          @Named(TaskExecutors.BLOCKING) ExecutorService blockingExecutor) {
        super(mcpHandler, contextExtractor, jsonMapper, mcpJsonMapper);
        this.toolRegistry = toolRegistry;
        this.promptRegistry = promptRegistry;
        this.resourceRegistry = resourceRegistry;
        this.blockingScheduler = Schedulers.fromExecutorService(blockingExecutor);
    }

    // The route returns a JSON-RPC response or an error body, so the response body type is a wildcard
    @SuppressWarnings({"java:S3740", "java:S1452"})
    @Post
    Mono<HttpResponse<?>> handlePost(HttpRequest<?> request, @Body @Nullable JsonNode body) {
        Mono<HttpResponse<?>> response = handle(request, body);
        if (mayStream(request, body)) {
            // The server writes a response only once the subscription to it returns, so a primitive that may stream
            // runs on another thread of the blocking executor, for its notifications to be written while it runs
            return response.subscribeOn(blockingScheduler);
        }
        // Subscribed to on this blocking thread, where the server invokes the primitive
        return response;
    }

    private boolean mayStream(HttpRequest<?> request, @Nullable JsonNode body) {
        if (body == null || !body.isObject() || !acceptsEventStream(request)) {
            return false;
        }
        JsonNode method = body.get("method");
        JsonNode params = body.get("params");
        if (method == null || !method.isString() || params == null || !params.isObject()) {
            return false;
        }
        return switch (method.getStringValue()) {
            case McpSchema.METHOD_TOOLS_CALL -> mayNotify(toolRegistry::mayNotify, params.get("name"));
            case McpSchema.METHOD_PROMPT_GET -> mayNotify(promptRegistry::mayNotify, params.get("name"));
            case McpSchema.METHOD_RESOURCES_READ -> mayNotify(resourceRegistry::mayNotify, params.get("uri"));
            default -> false;
        };
    }

    private static boolean mayNotify(Predicate<String> mayNotify, @Nullable JsonNode name) {
        return name != null && name.isString() && mayNotify.test(name.getStringValue());
    }
}
