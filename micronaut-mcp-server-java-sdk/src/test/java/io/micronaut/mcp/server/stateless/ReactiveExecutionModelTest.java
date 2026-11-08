package io.micronaut.mcp.server.stateless;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.context.ServerRequestContext;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.json.JsonMapper;
import io.micronaut.mcp.annotations.Tool;
import io.micronaut.mcp.server.tools.search.SearchResponse;
import io.micronaut.mcp.server.tools.search.SearchResult;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static io.micronaut.mcp.server.stateless.ExecutionModelSupport.callTool;
import static io.micronaut.mcp.server.stateless.ExecutionModelSupport.text;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Property(name = "micronaut.mcp.server.info.name", value = "mcp-server")
@Property(name = "micronaut.mcp.server.info.version", value = "0.0.1")
@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "micronaut.mcp.server.reactive", value = "true")
@Property(name = "spec.name", value = "ReactiveExecutionModelTest")
@Property(name = "execution-model.tools", value = "true")
@MicronautTest
class ReactiveExecutionModelTest {

    @Inject
    @Client("/")
    HttpClient httpClient;

    @Inject
    JsonMapper jsonMapper;

    @Test
    void toolsRunWhereTheyDeclare() throws IOException {
        assertEquals("true", text(callTool(httpClient, jsonMapper, "eventLoop")));
        assertEquals("false true", text(callTool(httpClient, jsonMapper, "blocking")));
    }

    @Test
    void executionModel() throws IOException {
        ExecutionModelAssertions.assertExecutionModel(httpClient, jsonMapper);
    }

    @Test
    void reactiveAndAsyncReturnValuesAreAwaited() throws IOException {
        assertEquals("from a publisher", text(callTool(httpClient, jsonMapper, "publisher")));
        assertEquals("from a future", text(callTool(httpClient, jsonMapper, "future")));
    }

    @Test
    void structuredOutputOfAPublisher() throws IOException {
        Map<String, Object> result = callTool(httpClient, jsonMapper, "structured");
        assertNotNull(result.get("structuredContent"));
        assertEquals("{\"results\":[{\"id\":\"1\",\"title\":\"Micronaut\",\"url\":\"https://micronaut.io\"}]}", text(result));
    }

    @Requires(property = "spec.name", value = "ReactiveExecutionModelTest")
    @Singleton
    static class Tools {
        @Tool
        String eventLoop() {
            return String.valueOf(ExecutionModelSupport.onEventLoop());
        }

        @Tool
        @ExecuteOn(TaskExecutors.BLOCKING)
        String blocking() {
            return ExecutionModelSupport.onEventLoop() + " " + ServerRequestContext.currentRequest().isPresent();
        }

        @Tool
        Mono<String> publisher() {
            return Mono.just("from a publisher");
        }

        @Tool
        CompletableFuture<String> future() {
            return CompletableFuture.supplyAsync(() -> "from a future");
        }

        @Tool
        Mono<SearchResponse> structured() {
            return Mono.just(new SearchResponse(List.of(new SearchResult("1", "Micronaut", "https://micronaut.io"))));
        }
    }
}
