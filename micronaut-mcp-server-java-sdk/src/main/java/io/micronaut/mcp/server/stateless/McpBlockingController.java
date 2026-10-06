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
import io.micronaut.core.propagation.PropagatedContext;
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
import jakarta.inject.Named;
import org.jspecify.annotations.Nullable;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

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
    private final McpStreamingPolicy streamingPolicy;
    private final Scheduler blockingScheduler;

    McpBlockingController(McpStatelessServerHandler mcpHandler,
                          McpTransportContextExtractor<HttpRequest<?>> contextExtractor,
                          JsonMapper jsonMapper,
                          McpJsonMapper mcpJsonMapper,
                          McpStreamingPolicy streamingPolicy,
                          @Named(TaskExecutors.BLOCKING) ExecutorService blockingExecutor) {
        super(mcpHandler, contextExtractor, jsonMapper, mcpJsonMapper);
        this.streamingPolicy = streamingPolicy;
        // The propagated context of the request, with the request itself, follows the hop to another blocking thread
        this.blockingScheduler = Schedulers.fromExecutor(task -> blockingExecutor.execute(PropagatedContext.wrapCurrent(task)));
    }

    // The route returns a JSON-RPC response or an error body, so the response body type is a wildcard
    @SuppressWarnings({"java:S3740", "java:S1452"})
    @Post
    CompletableFuture<@Nullable HttpResponse<?>> handlePost(HttpRequest<?> request, @Body @Nullable JsonNode body) {
        if (streamingPolicy.mayStream(request, body)) {
            // The server writes a response only once the route returns, so a primitive that may stream runs on another
            // thread of the blocking executor, and the response completes with its first notification or its result
            return handle(request, body, true).subscribeOn(blockingScheduler).toFuture();
        }
        // The server invokes the primitive on this blocking thread
        return CompletableFuture.completedFuture(handle(request, body, false).block());
    }
}
