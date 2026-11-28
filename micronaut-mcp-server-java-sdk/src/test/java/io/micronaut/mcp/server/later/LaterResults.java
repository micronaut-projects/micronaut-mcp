package io.micronaut.mcp.server.later;

import io.micronaut.context.annotation.Requires;
import io.micronaut.mcp.annotations.Prompt;
import io.micronaut.mcp.annotations.PromptCompletion;
import io.micronaut.mcp.annotations.Resource;
import io.micronaut.mcp.annotations.ResourceTemplate;
import io.micronaut.mcp.annotations.Tool;
import io.micronaut.mcp.server.stateless.sync.EvaluationResult;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Singleton;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

/**
 * Tools, prompts, resources and completions whose methods answer later: with a future or a publisher.
 */
@Requires(property = "spec.name", value = "LaterResultsTest")
@Singleton
class LaterResults {

    private static <T> CompletableFuture<T> later(T value) {
        return CompletableFuture.supplyAsync(() -> value, CompletableFuture.delayedExecutor(20, TimeUnit.MILLISECONDS));
    }

    @Tool(description = "A future of a text.")
    CompletableFuture<String> futureText(String fen) {
        return later("+0.12 for " + fen);
    }

    @Tool(description = "A stage of a text.")
    CompletionStage<String> stageText(String fen) {
        return later("+0.13 for " + fen);
    }

    @Tool(description = "A publisher of one text.")
    Mono<String> monoText(String fen) {
        return Mono.delay(Duration.ofMillis(20)).map(tick -> "+0.14 for " + fen);
    }

    @Tool(description = "A publisher of several texts.")
    Flux<String> fluxTexts(String fen) {
        return Flux.just("e4", "d4").delayElements(Duration.ofMillis(5));
    }

    @Tool(description = "A publisher of several texts, declared as the interface.")
    Publisher<String> publisherTexts(String fen) {
        return Flux.just("c4", "Nf3");
    }

    @Tool(description = "A future of an object that has a schema.")
    CompletableFuture<EvaluationResult> futureObject(String fen) {
        return later(new EvaluationResult(fen, "+0.12"));
    }

    @Tool(description = "A future of a result.")
    CompletableFuture<McpSchema.CallToolResult> futureResult(String fen) {
        return later(McpSchema.CallToolResult.builder().addTextContent("built for " + fen).isError(false).build());
    }

    @Tool(description = "A future that fails.")
    CompletableFuture<String> futureFails(String fen) {
        return CompletableFuture.supplyAsync(() -> {
            throw new IllegalStateException("no evaluation of " + fen);
        }, CompletableFuture.delayedExecutor(20, TimeUnit.MILLISECONDS));
    }

    @Tool(description = "A publisher that completes with nothing.")
    Mono<String> emptyMono(String fen) {
        return Mono.empty();
    }

    @Tool(description = "A future of a result that completes with nothing.")
    CompletableFuture<McpSchema.CallToolResult> futureOfNoResult(String fen) {
        return later(null);
    }

    @Tool(description = "A method that fails as it is called.")
    String fails(String fen) {
        throw new IllegalStateException("no evaluation of " + fen);
    }

    @Prompt(name = "future-prompt", description = "A future of a prompt")
    CompletableFuture<String> futurePrompt(String name) {
        return later("Statistics for " + name);
    }

    @Prompt(name = "mono-prompt", description = "A publisher of a prompt")
    Mono<String> monoPrompt(String name) {
        return Mono.delay(Duration.ofMillis(20)).map(tick -> "Games of " + name);
    }

    @Resource(uri = "later://hello", name = "hello", mimeType = "text/plain")
    CompletableFuture<String> futureResource() {
        return later("Hello later");
    }

    @Resource(uri = "later://mono", name = "mono", mimeType = "text/plain")
    Mono<String> monoResource() {
        return Mono.delay(Duration.ofMillis(20)).map(tick -> "Hello mono");
    }

    @ResourceTemplate(uriTemplate = "later://round/{round}", name = "round", mimeType = "text/plain")
    CompletableFuture<String> futureTemplate(Integer round) {
        return later("Round " + round);
    }

    @PromptCompletion(name = "future-prompt")
    CompletableFuture<List<String>> futureCompletion(String name) {
        return later(List.of(name + "gio", name + "ena"));
    }

    @PromptCompletion(name = "mono-prompt")
    Flux<String> fluxCompletion(String name) {
        return Flux.just(name + "a", name + "b");
    }
}
