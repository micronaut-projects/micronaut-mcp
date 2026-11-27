package io.micronaut.mcp.server.stateless;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.context.ServerRequestContext;
import io.micronaut.http.filter.HttpServerFilter;
import io.micronaut.http.filter.ServerFilterChain;
import io.micronaut.json.JsonMapper;
import io.micronaut.mcp.annotations.Tool;
import io.micronaut.mcp.server.tools.search.SearchResponse;
import io.micronaut.mcp.server.tools.search.SearchResult;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static io.micronaut.mcp.server.stateless.ExecutionModelSupport.callTool;
import static io.micronaut.mcp.server.stateless.ExecutionModelSupport.text;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Property(name = "micronaut.mcp.server.info.name", value = "mcp-server")
@Property(name = "micronaut.mcp.server.info.version", value = "0.0.1")
@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "spec.name", value = "BlockingExecutionModelTest")
@Property(name = "execution-model.tools", value = "true")
@Property(name = "micronaut.executors.tool-worker.type", value = "fixed")
@Property(name = "micronaut.executors.tool-worker.n-threads", value = "1")
@MicronautTest
class BlockingExecutionModelTest {

    @Inject
    @Client("/")
    HttpClient httpClient;

    @Inject
    JsonMapper jsonMapper;

    @Test
    void toolsRunOffTheEventLoopWithTheRequestInScope() throws IOException {
        assertEquals("false true", text(callTool(httpClient, jsonMapper, "where")));
        // a method on the blocking executor, which the controller already runs on, is called without a hop
        assertEquals("true", text(callTool(httpClient, jsonMapper, "onBlocking")));
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
    void filterContextReachesTheToolPublisher() throws IOException {
        assertEquals("from filter", text(callTool(httpClient, jsonMapper, "filterContext")));
    }

    @Test
    void stageCanCompleteOnTheToolExecutor() throws IOException {
        assertEquals("same worker", text(callTool(httpClient, jsonMapper, "stageOnToolWorker")));
    }

    @Test
    void structuredOutputOfAPublisher() throws IOException {
        Map<String, Object> result = callTool(httpClient, jsonMapper, "structured");
        assertNotNull(result.get("structuredContent"));
        assertEquals("{\"results\":[{\"id\":\"1\",\"title\":\"Micronaut\",\"url\":\"https://micronaut.io\"}]}", text(result));
    }

    @Requires(property = "spec.name", value = "BlockingExecutionModelTest")
    @Filter("/mcp")
    static class ContextFilter implements HttpServerFilter {
        @Override
        public Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain) {
            return Mono.from(chain.proceed(request)).contextWrite(ctx -> ctx.put("filter-value", "from filter"));
        }
    }

    @Requires(property = "spec.name", value = "BlockingExecutionModelTest")
    @Singleton
    static class Tools {
        private final ExecutorService worker;

        Tools(@Named("tool-worker") ExecutorService worker) {
            this.worker = worker;
        }

        @Tool
        Mono<String> filterContext() {
            return Mono.deferContextual(ctx -> Mono.just(ctx.get("filter-value")));
        }

        @Tool
        @ExecuteOn("tool-worker")
        CompletableFuture<String> stageOnToolWorker() {
            Thread caller = Thread.currentThread();
            return CompletableFuture.supplyAsync(() -> caller == Thread.currentThread() ? "same worker" : "different worker", worker)
                .orTimeout(5, TimeUnit.SECONDS);
        }

        @Tool
        String where() {
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
