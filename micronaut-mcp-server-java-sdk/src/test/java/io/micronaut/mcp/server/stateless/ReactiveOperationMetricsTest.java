package io.micronaut.mcp.server.stateless;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.mcp.annotations.Prompt;
import io.micronaut.mcp.annotations.PromptCompletion;
import io.micronaut.mcp.annotations.Resource;
import io.micronaut.mcp.annotations.ResourceCompletion;
import io.micronaut.mcp.annotations.ResourceTemplate;
import io.micronaut.mcp.annotations.Tool;
import io.micronaut.mcp.server.observability.McpServerObserver;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Singleton;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

@Property(name = "micronaut.mcp.server.info.name", value = "mcp-server")
@Property(name = "micronaut.mcp.server.info.version", value = "0.0.1")
@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "micronaut.mcp.server.reactive", value = "true")
@Property(name = "spec.name", value = "ReactiveOperationMetricsTest")
@MicronautTest
class ReactiveOperationMetricsTest extends AbstractOperationMetricsTest {

    @Requires(property = "spec.name", value = "ReactiveOperationMetricsTest")
    @Singleton
    static class Primitives {
        @Tool
        Mono<Integer> add(int a, int b) {
            return Mono.just(a + b);
        }

        @Tool
        Mono<String> broken() {
            return Mono.error(new IllegalStateException("broken"));
        }

        @Tool
        Mono<String> invalid() {
            return Mono.error(invalidParams());
        }

        @Tool
        Mono<String> relay(int code) {
            return Mono.error(error(code));
        }

        @Tool
        Mono<String> slow() {
            return Mono.delay(Duration.ofMillis(SLOW_MILLIS)).thenReturn("done");
        }

        @Prompt
        String greet() {
            return "Hello";
        }

        @Prompt
        Mono<String> brokenPrompt() {
            return Mono.error(new IllegalStateException("broken"));
        }

        @Prompt
        Mono<String> count(int n) {
            return Mono.just("Count to " + n);
        }

        @Prompt
        String translate(String language) {
            return "Translate to " + language;
        }

        @PromptCompletion(name = "translate")
        Mono<List<String>> languages() {
            return Mono.just(List.of("python"));
        }

        @ResourceTemplate(uriTemplate = "metrics://cities/{city}")
        Mono<String> city(String city) {
            return Mono.just(city);
        }

        @ResourceCompletion(uri = "metrics://cities/{city}")
        Mono<List<String>> cities() {
            return Mono.just(List.of("paris"));
        }

        @Resource(uri = "metrics://broken")
        String brokenResource() throws IOException {
            throw new IOException("unreadable");
        }
    }

    /**
     * An observer that fails to stop, which must neither fail the operations nor record them twice.
     */
    @Requires(property = "spec.name", value = "ReactiveOperationMetricsTest")
    @Singleton
    static class FailingObserver implements McpServerObserver {
        @Override
        public Observation start(String method, String name) {
            return errorType -> {
                throw new IllegalStateException("the observer is broken");
            };
        }
    }
}
