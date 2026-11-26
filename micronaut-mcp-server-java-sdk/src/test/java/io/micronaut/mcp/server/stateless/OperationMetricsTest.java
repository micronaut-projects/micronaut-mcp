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

import java.io.IOException;
import java.util.List;

@Property(name = "micronaut.mcp.server.info.name", value = "mcp-server")
@Property(name = "micronaut.mcp.server.info.version", value = "0.0.1")
@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "spec.name", value = "OperationMetricsTest")
@MicronautTest
class OperationMetricsTest extends AbstractOperationMetricsTest {

    @Requires(property = "spec.name", value = "OperationMetricsTest")
    @Singleton
    static class Primitives {
        @Tool
        int add(int a, int b) {
            return a + b;
        }

        @Tool
        String broken() {
            throw new IllegalStateException("broken");
        }

        @Tool
        String invalid() {
            throw invalidParams();
        }

        @Tool
        String relay(int code) {
            throw error(code);
        }

        @Tool
        String slow() throws InterruptedException {
            Thread.sleep(SLOW_MILLIS);
            return "done";
        }

        @Prompt
        String greet() {
            return "Hello";
        }

        @Prompt
        String brokenPrompt() {
            throw new IllegalStateException("broken");
        }

        @Prompt
        String count(int n) {
            return "Count to " + n;
        }

        @Prompt
        String translate(String language) {
            return "Translate to " + language;
        }

        @PromptCompletion(name = "translate")
        List<String> languages() {
            return List.of("python");
        }

        @ResourceTemplate(uriTemplate = "metrics://cities/{city}")
        String city(String city) {
            return city;
        }

        @ResourceCompletion(uri = "metrics://cities/{city}")
        List<String> cities() {
            return List.of("paris");
        }

        @Resource(uri = "metrics://broken")
        String brokenResource() throws IOException {
            throw new IOException("unreadable");
        }
    }

    /**
     * An observer that fails to start, which must neither fail the operations nor keep the timers from being recorded.
     */
    @Requires(property = "spec.name", value = "OperationMetricsTest")
    @Singleton
    static class FailingObserver implements McpServerObserver {
        @Override
        public Observation start(String method, String name) {
            throw new IllegalStateException("the observer is broken");
        }
    }
}
