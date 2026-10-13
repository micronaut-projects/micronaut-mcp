package io.micronaut.mcp.server.stateless;

import io.micronaut.context.annotation.Requires;
import io.micronaut.http.context.ServerRequestContext;
import io.micronaut.mcp.annotations.Prompt;
import io.micronaut.mcp.annotations.PromptCompletion;
import io.micronaut.mcp.annotations.Tool;
import io.micronaut.mcp.server.tools.search.SearchResponse;
import io.micronaut.mcp.server.tools.search.SearchResult;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Singleton;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Primitives shared by the blocking and the reactive execution model tests.
 */
@Requires(property = "execution-model.tools")
@Singleton
class ExecutionModelTools {
    @Tool
    Mono<?> wildcardMono() {
        return Mono.just("wildcard");
    }

    @Tool
    Object pojoAsObject() {
        return new SearchResult("1", "Micronaut", "https://micronaut.io");
    }

    @Tool
    Publisher<String> twoValues() {
        return Flux.just("a", "b");
    }

    @Tool
    Mono<String> invalidParams() {
        return Mono.error(McpError.builder(McpSchema.ErrorCodes.INVALID_PARAMS).message("bad params").build());
    }

    @Tool
    Mono<String> reactorContext() {
        return Mono.deferContextual(ctx -> Mono.just(String.valueOf(ctx.hasKey(McpTransportContext.KEY))));
    }

    @Tool
    Mono<SearchResponse> emptyStructured() {
        return Mono.empty();
    }

    @Tool
    @ExecuteOn(TaskExecutors.IO)
    String onIo() {
        return Thread.currentThread().getName().contains("io-executor") + " " + ServerRequestContext.currentRequest().isPresent();
    }

    @Prompt
    String greet(String name) {
        return "Hello " + name;
    }

    @PromptCompletion(name = "greet")
    Flux<String> greetCompletion() {
        return Flux.just("Alice", "Bob");
    }
}
