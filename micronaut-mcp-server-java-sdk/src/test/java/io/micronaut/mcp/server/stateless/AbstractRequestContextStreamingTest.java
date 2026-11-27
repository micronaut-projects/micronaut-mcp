package io.micronaut.mcp.server.stateless;

import io.micronaut.context.annotation.Requires;
import io.micronaut.core.propagation.PropagatedContext;
import io.micronaut.core.propagation.PropagatedContextElement;
import io.micronaut.core.util.StringUtils;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.HttpServerFilter;
import io.micronaut.http.filter.ServerFilterChain;
import io.netty.util.concurrent.FastThreadLocalThread;
import org.reactivestreams.Publisher;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.http.context.ServerRequestContext;
import io.micronaut.json.JsonMapper;
import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.mcp.annotations.Prompt;
import io.micronaut.mcp.annotations.PromptCompletion;
import io.micronaut.mcp.annotations.Resource;
import io.micronaut.mcp.annotations.ResourceCompletion;
import io.micronaut.mcp.annotations.ResourceTemplate;
import io.micronaut.mcp.annotations.Tool;
import io.micronaut.mcp.server.context.McpRequestContext;
import io.micronaut.mcp.server.context.MicronautMcpTransportContext;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The notifications of a primitive with an {@link McpRequestContext} over HTTP. Subclasses run the scenarios on the
 * blocking and on the reactive controller.
 */
abstract class AbstractRequestContextStreamingTest {
    static final String PROPERTY = "request-context-streaming-test";
    static final String ACCEPT_BOTH = "application/json, text/event-stream";

    /**
     * @return Whether the controller invokes a method without an executor on the event loop, as the reactive one does
     */
    abstract boolean invokesOnTheEventLoop();

    @Inject
    @Client("/")
    HttpClient httpClient;

    @Inject
    EmbeddedServer server;

    @Inject
    JsonMapper jsonMapper;

    @Test
    void notificationsUpgradeTheResponseToAStream() {
        HttpResponse<String> response = post(ACCEPT_BOTH, """
            {"jsonrpc": "2.0", "id": 7, "method": "tools/call",
             "params": {"name": "importCatalog", "arguments": {"pages": 2}, "_meta": {"progressToken": "p1"}}}""");
        assertTrue(response.getContentType().map(MediaType.TEXT_EVENT_STREAM_TYPE::matches).orElse(false), response.getContentType().toString());
        // No cache or reverse proxy may hold the events back
        assertEquals("no-cache, no-transform", response.getHeaders().get("Cache-Control"));
        assertEquals("no", response.getHeaders().get("X-Accel-Buffering"));
        List<String> events = data(response.body());
        assertEquals(4, events.size(), response.body());
        assertTrue(events.get(0).contains("\"method\":\"notifications/progress\"") && events.get(0).contains("\"progress\":1.0")
            && events.get(0).contains("\"progressToken\":\"p1\"") && events.get(0).contains("\"total\":2.0"), events.get(0));
        assertTrue(events.get(1).contains("\"progress\":2.0"), events.get(1));
        assertTrue(events.get(2).contains("\"method\":\"notifications/message\"") && events.get(2).contains("\"level\":\"info\"")
            && events.get(2).contains("imported 2 pages"), events.get(2));
        assertTrue(events.get(3).contains("\"id\":7") && events.get(3).contains("\"text\":\"done\""), events.get(3));
    }

    @Test
    void withoutAProgressTokenOnlyLogMessagesAreSent() {
        HttpResponse<String> response = post(ACCEPT_BOTH, """
            {"jsonrpc": "2.0", "id": 8, "method": "tools/call", "params": {"name": "importCatalog", "arguments": {"pages": 2}}}""");
        List<String> events = data(response.body());
        assertEquals(2, events.size(), response.body());
        assertTrue(events.get(0).contains("notifications/message"), events.get(0));
    }

    @Test
    void aProgressTokenThatIsNotAStringOrAnIntegerIsIgnored() {
        List<String> events = data(post(ACCEPT_BOTH, """
            {"jsonrpc": "2.0", "id": 34, "method": "tools/call",
             "params": {"name": "importCatalog", "arguments": {"pages": 2}, "_meta": {"progressToken": {"o": "AAAA"}}}}""").body());
        assertEquals(2, events.size(), String.valueOf(events));
        assertTrue(events.get(0).contains("notifications/message"), events.get(0));
    }

    @Test
    void aClientThatOnlyAcceptsJsonGetsJson() {
        HttpResponse<String> response = post(MediaType.APPLICATION_JSON, """
            {"jsonrpc": "2.0", "id": 9, "method": "tools/call",
             "params": {"name": "importCatalog", "arguments": {"pages": 2}, "_meta": {"progressToken": "p1"}}}""");
        assertTrue(response.getContentType().map(MediaType.APPLICATION_JSON_TYPE::matches).orElse(false));
        assertTrue(response.body().contains("\"text\":\"done\"") && !response.body().contains("notifications/"), response.body());
    }

    @Test
    void aRequestWithoutNotificationsStaysJson() {
        HttpResponse<String> response = post(ACCEPT_BOTH, """
            {"jsonrpc": "2.0", "id": 10, "method": "tools/call", "params": {"name": "quiet", "arguments": {}}}""");
        assertTrue(response.getContentType().map(MediaType.APPLICATION_JSON_TYPE::matches).orElse(false));
        assertTrue(response.body().contains("\"text\":\"quiet\""), response.body());
    }

    @Test
    void promptsAndResourcesStreamTheirNotifications() {
        List<String> prompt = data(post(ACCEPT_BOTH, """
            {"jsonrpc": "2.0", "id": 14, "method": "prompts/get", "params": {"name": "explain", "arguments": {"topic": "mcp"}}}""").body());
        assertEquals(2, prompt.size(), String.valueOf(prompt));
        assertTrue(prompt.get(0).contains("explaining mcp for explain"), prompt.get(0));
        List<String> resource = data(post(ACCEPT_BOTH, """
            {"jsonrpc": "2.0", "id": 15, "method": "resources/read", "params": {"uri": "catalog://status"}}""").body());
        assertEquals(2, resource.size(), String.valueOf(resource));
        assertTrue(resource.get(0).contains("reading the status") && resource.get(1).contains("\"text\":\"ok\""), String.valueOf(resource));
    }

    @Test
    void resourceTemplatesAndCompletionsStreamTheirNotifications() {
        List<String> item = data(post(ACCEPT_BOTH, """
            {"jsonrpc": "2.0", "id": 16, "method": "resources/read", "params": {"uri": "catalog://items/42"}}""").body());
        assertEquals(2, item.size(), String.valueOf(item));
        assertTrue(item.get(0).contains("reading item 42") && item.get(1).contains("\"text\":\"item 42\""), String.valueOf(item));
        List<String> completion = data(post(ACCEPT_BOTH, """
            {"jsonrpc": "2.0", "id": 17, "method": "completion/complete",
             "params": {"ref": {"type": "ref/prompt", "name": "explain"}, "argument": {"name": "topic", "value": "m"}}}""").body());
        assertEquals(2, completion.size(), String.valueOf(completion));
        assertTrue(completion.get(0).contains("completing topics") && completion.get(1).contains("\"mcp\""), String.valueOf(completion));
        List<String> resourceCompletion = data(post(ACCEPT_BOTH, """
            {"jsonrpc": "2.0", "id": 23, "method": "completion/complete",
             "params": {"ref": {"type": "ref/resource", "uri": "catalog://items/{id}"}, "argument": {"name": "id", "value": "4"}}}""").body());
        assertEquals(2, resourceCompletion.size(), String.valueOf(resourceCompletion));
        assertTrue(resourceCompletion.get(0).contains("completing ids") && resourceCompletion.get(1).contains("\"42\""), String.valueOf(resourceCompletion));
    }

    @Test
    void aCompletionWithoutAReferencedPrimitiveStaysJson() {
        // Without a type, the name of a prompt reference does not designate the prompt
        for (String ref : List.of("{\"type\": \"ref/prompt\"}", "{\"type\": \"ref/resource\"}", "{\"name\": \"explain\"}",
                "{\"type\": \"ref/other\", \"name\": \"explain\"}", "\"explain\"")) {
            HttpClientResponseException e = assertThrows(HttpClientResponseException.class, () -> post(ACCEPT_BOTH, """
                {"jsonrpc": "2.0", "id": 24, "method": "completion/complete", "params": {"ref": %s, "argument": {"name": "topic", "value": "m"}}}"""
                .formatted(ref)));
            assertTrue(e.getResponse().getContentType().map(MediaType.APPLICATION_JSON_TYPE::matches).orElse(false), ref);
        }
    }

    @Test
    void theRequestIsInScopeOfAStreamingPrimitive() {
        List<String> events = data(post(ACCEPT_BOTH, """
            {"jsonrpc": "2.0", "id": 18, "method": "tools/call", "params": {"name": "requestInScope", "arguments": {}}}""").body());
        assertEquals(2, events.size(), String.valueOf(events));
        assertTrue(events.get(1).contains("\"text\":\"true\""), events.get(1));
    }

    @Test
    void aClientThatDoesNotAcceptBothGetsJson() {
        // An event stream with a quality of 0 is not accepted, and a POST request must accept JSON too
        for (String accept : List.of("application/json, text/event-stream;q=0", "text/event-stream, */*")) {
            HttpResponse<String> response = post(accept, """
                {"jsonrpc": "2.0", "id": 19, "method": "tools/call", "params": {"name": "importCatalog", "arguments": {"pages": 1}}}""");
            assertTrue(response.getContentType().map(MediaType.APPLICATION_JSON_TYPE::matches).orElse(false), accept);
            assertTrue(response.body().contains("\"text\":\"done\"") && !response.body().contains("notifications/"), response.body());
        }
        // The endpoint produces JSON, so the server refuses a client that only accepts an event stream
        HttpClientResponseException e = assertThrows(HttpClientResponseException.class, () -> post("text/event-stream", """
            {"jsonrpc": "2.0", "id": 20, "method": "tools/call", "params": {"name": "importCatalog", "arguments": {"pages": 1}}}"""));
        assertEquals(HttpStatus.NOT_ACCEPTABLE, e.getStatus());
    }

    @Test
    void aFailureAfterANotificationIsTheLastEvent() {
        // A tool that fails produces a tool result with an error
        List<String> tool = data(post(ACCEPT_BOTH, """
            {"jsonrpc": "2.0", "id": 30, "method": "tools/call", "params": {"name": "notifyThenThrow", "arguments": {}}}""").body());
        assertEquals(2, tool.size(), String.valueOf(tool));
        assertTrue(tool.get(0).contains("notifications/message"), tool.get(0));
        assertTrue(tool.get(1).contains("\"id\":30") && tool.get(1).contains("\"isError\":true") && tool.get(1).contains("Tool notifyThenThrow failed"), tool.get(1));
        // A prompt that fails produces a JSON-RPC error without the details of the exception, in a response that is already a stream with the status 200
        HttpResponse<String> response = post(ACCEPT_BOTH, """
            {"jsonrpc": "2.0", "id": 31, "method": "prompts/get", "params": {"name": "failingPrompt", "arguments": {}}}""");
        assertEquals(HttpStatus.OK, response.getStatus());
        List<String> prompt = data(response.body());
        assertEquals(2, prompt.size(), String.valueOf(prompt));
        assertTrue(prompt.get(1).contains("\"id\":31") && prompt.get(1).contains("\"error\"") && prompt.get(1).contains("Internal error"), prompt.get(1));
    }

    @Test
    void elicitationAndSamplingAreNotSupportedOverHttp() {
        for (String tool : List.of("elicitOverHttp", "sampleOverHttp")) {
            String body = post(ACCEPT_BOTH, """
                {"jsonrpc": "2.0", "id": 32, "method": "tools/call", "params": {"name": "%s", "arguments": {}}}""".formatted(tool)).body();
            String result = data(body).getLast();
            // the reason, which is meant for the developer, is logged
            assertTrue(result.contains("\"isError\":true") && result.contains("Tool " + tool + " failed"), result);
        }
    }

    @Test
    void thePropagatedContextReachesThePrimitive() {
        // Streaming, the blocking controller runs the primitive on another blocking thread; as JSON, on the route's own
        for (String accept : List.of(ACCEPT_BOTH, MediaType.APPLICATION_JSON)) {
            String body = post(accept, """
                {"jsonrpc": "2.0", "id": 33, "method": "tools/call", "params": {"name": "propagated", "arguments": {}}}""").body();
            String result = data(body).isEmpty() ? body : data(body).getLast();
            assertTrue(result.contains("\"text\":\"marker=true, eventLoop=" + invokesOnTheEventLoop() + "\""), accept + ": " + result);
        }
    }

    @Test
    void theLoggingCapabilityIsAdvertised() {
        String initialize = post(MediaType.APPLICATION_JSON, """
            {"jsonrpc": "2.0", "id": 20, "method": "initialize",
             "params": {"protocolVersion": "2025-06-18", "capabilities": {}, "clientInfo": {"name": "test", "version": "1"}}}""").body();
        assertTrue(initialize.contains("\"logging\":{}"), initialize);
    }

    @ParameterizedTest
    @EnumSource(McpSchema.LoggingLevel.class)
    void settingEachLogLevelIsAcknowledged(McpSchema.LoggingLevel level) throws IOException {
        // The protocol spells the levels in lower case; a client that accepts an event stream gets JSON too
        HttpResponse<String> response = post(ACCEPT_BOTH, """
            {"jsonrpc": "2.0", "id": 21, "method": "logging/setLevel", "params": {"level": "%s"}}"""
            .formatted(level.name().toLowerCase(Locale.ROOT)));
        assertTrue(response.getContentType().map(MediaType.APPLICATION_JSON_TYPE::matches).orElse(false));
        Map<?, ?> body = json(response.body());
        assertEquals(21, ((Number) body.get("id")).intValue());
        assertEquals(Map.of(), body.get("result"));
        assertFalse(body.containsKey("error"), response.body());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "\"params\": {\"level\": \"verbose\"}",
        "\"params\": {\"level\": \"INFO\"}",
        "\"params\": {\"level\": 3}",
        "\"params\": {}",
        "\"params\": []",
        "\"params\": null",
        "\"other\": 1"
    })
    void settingAnInvalidLogLevelIsRejected(String params) throws IOException {
        HttpClientResponseException e = assertThrows(HttpClientResponseException.class, () -> post(MediaType.APPLICATION_JSON, """
            {"jsonrpc": "2.0", "id": 22, "method": "logging/setLevel", %s}""".formatted(params)));
        assertEquals(HttpStatus.BAD_REQUEST, e.getStatus(), params);
        Map<?, ?> body = json(e.getResponse().getBody(String.class).orElseThrow());
        assertEquals(22, ((Number) body.get("id")).intValue());
        assertEquals(McpSchema.ErrorCodes.INVALID_PARAMS, ((Number) ((Map<?, ?>) body.get("error")).get("code")).intValue(), params);
    }

    @Test
    void theRequestContextIsNotPartOfTheInputSchemaOrPromptArguments() throws IOException {
        Map<?, ?> tools = (Map<?, ?>) json(post(MediaType.APPLICATION_JSON, """
            {"jsonrpc": "2.0", "id": 11, "method": "tools/list", "params": {}}""").body()).get("result");
        Map<?, ?> importCatalog = ((List<?>) tools.get("tools")).stream()
            .map(Map.class::cast)
            .filter(tool -> "importCatalog".equals(tool.get("name")))
            .findFirst().orElseThrow();
        Map<?, ?> inputSchema = (Map<?, ?>) importCatalog.get("inputSchema");
        assertEquals(Set.of("pages"), ((Map<?, ?>) inputSchema.get("properties")).keySet());
        assertEquals(List.of("pages"), inputSchema.get("required"));
        Map<?, ?> prompts = (Map<?, ?>) json(post(MediaType.APPLICATION_JSON, """
            {"jsonrpc": "2.0", "id": 12, "method": "prompts/list", "params": {}}""").body()).get("result");
        Map<?, ?> explain = ((List<?>) prompts.get("prompts")).stream()
            .map(Map.class::cast)
            .filter(prompt -> "explain".equals(prompt.get("name")))
            .findFirst().orElseThrow();
        assertEquals(List.of("topic"), ((List<?>) explain.get("arguments")).stream().map(a -> ((Map<?, ?>) a).get("name")).toList());
    }

    private Map<?, ?> json(String body) throws IOException {
        return jsonMapper.readValue(body, Map.class);
    }

    @Test
    void notificationsReachTheClientWhileTheToolRuns() throws Exception {
        Primitives.CLIENT_GOT_PROGRESS = new CountDownLatch(1);
        java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder(URI.create(server.getURL() + "/mcp"))
            .header("Content-Type", MediaType.APPLICATION_JSON)
            .header("Accept", ACCEPT_BOTH)
            .POST(java.net.http.HttpRequest.BodyPublishers.ofString("""
                {"jsonrpc": "2.0", "id": 13, "method": "tools/call",
                 "params": {"name": "waitForClient", "arguments": {}, "_meta": {"progressToken": 1}}}"""))
            .build();
        // The JDK client hands over each line as it arrives
        try (java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient()) {
            List<String> events = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofLines()).body()
                .filter(line -> line.startsWith("data:"))
                .peek(line -> {
                    if (line.contains("notifications/progress")) {
                        Primitives.CLIENT_GOT_PROGRESS.countDown();
                    }
                })
                .toList();
            assertEquals(2, events.size(), String.valueOf(events));
            assertTrue(events.get(1).contains("\"text\":\"the client saw the progress\""), events.get(1));
        }
    }

    @Test
    void closingTheEventStreamCancelsTheRequest() throws Exception {
        Primitives.CANCELLED = new CompletableFuture<>();
        java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder(URI.create(server.getURL() + "/mcp"))
            .header("Content-Type", MediaType.APPLICATION_JSON)
            .header("Accept", ACCEPT_BOTH)
            .POST(java.net.http.HttpRequest.BodyPublishers.ofString("""
                {"jsonrpc": "2.0", "id": 25, "method": "tools/call",
                 "params": {"name": "notifyUntilCancelled", "arguments": {}, "_meta": {"progressToken": 1}}}"""))
            .build();
        java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
        try (InputStream events = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofInputStream()).body()) {
            // The first notification has arrived, so the response is an event stream
            assertTrue(events.read() >= 0);
        } finally {
            client.shutdownNow();
        }
        assertTrue(Primitives.CANCELLED.get(10, TimeUnit.SECONDS), "the tool saw the request cancelled");
    }

    private HttpResponse<String> post(String accept, String body) {
        return httpClient.toBlocking().exchange(HttpRequest.POST("/mcp", body)
            .contentType(MediaType.APPLICATION_JSON_TYPE)
            .header("Accept", accept), String.class);
    }

    private static List<String> data(String stream) {
        return Stream.of(stream.split("\n"))
            .filter(line -> line.startsWith("data:"))
            .map(line -> line.substring("data:".length()).trim())
            .toList();
    }

    @Requires(property = PROPERTY, value = StringUtils.TRUE)
    @Singleton
    static class Primitives {
        @Tool
        String importCatalog(int pages, McpRequestContext context) {
            for (int page = 1; page <= pages; page++) {
                context.progress(page, pages, "Imported page " + page);
            }
            context.log(McpSchema.LoggingLevel.INFO, "catalog", "imported " + pages + " pages");
            return "done";
        }

        // Counted down once per run of the test, which replaces it
        static volatile CountDownLatch CLIENT_GOT_PROGRESS = new CountDownLatch(1);

        @Tool
        @ExecuteOn(TaskExecutors.BLOCKING)
        String waitForClient(McpRequestContext context) throws InterruptedException {
            context.progress(1);
            return CLIENT_GOT_PROGRESS.await(10, TimeUnit.SECONDS) ? "the client saw the progress" : "the progress was not streamed";
        }

        static volatile CompletableFuture<Boolean> CANCELLED = new CompletableFuture<>();

        @Tool
        @ExecuteOn(TaskExecutors.BLOCKING)
        String notifyUntilCancelled(McpRequestContext context) {
            try {
                for (int i = 1; i <= 500 && !context.isCancelled(); i++) {
                    context.progress(i);
                    Thread.sleep(20);
                }
            } catch (InterruptedException e) {
                // The reactive server interrupts a method that runs on an executor when its request is cancelled
                Thread.currentThread().interrupt();
            }
            CANCELLED.complete(context.isCancelled());
            return "done";
        }

        @Tool
        String notifyThenThrow(McpRequestContext context) {
            context.log(McpSchema.LoggingLevel.INFO, "importing");
            throw new IllegalStateException("import failed");
        }

        @Prompt
        String failingPrompt(McpRequestContext context) {
            context.log(McpSchema.LoggingLevel.INFO, "preparing");
            throw new IllegalStateException("prompt failed");
        }

        @Tool
        String elicitOverHttp(McpRequestContext context) {
            context.log(McpSchema.LoggingLevel.INFO, "asking");
            return context.elicit(McpSchema.ElicitRequest.builder().message("Name?").requestedSchema(Map.of("type", "object")).build()).toString();
        }

        @Tool
        String sampleOverHttp(McpRequestContext context) {
            context.log(McpSchema.LoggingLevel.INFO, "sampling");
            return context.sample(McpSchema.CreateMessageRequest.builder().messages(List.of()).maxTokens(1).build()).toString();
        }

        @Tool
        String propagated(McpRequestContext context) {
            context.log(McpSchema.LoggingLevel.INFO, "checking the propagated context");
            return "marker=" + PropagatedContext.getOrEmpty().find(Marker.class).isPresent()
                + ", eventLoop=" + (Thread.currentThread() instanceof FastThreadLocalThread);
        }

        @Tool
        String quiet(McpRequestContext context) {
            return "quiet";
        }

        @Resource(uri = "catalog://status")
        String status(McpRequestContext context) {
            context.log(McpSchema.LoggingLevel.INFO, "reading the status");
            return "ok";
        }

        @ResourceTemplate(uriTemplate = "catalog://items/{id}")
        String item(String id, McpRequestContext context) {
            context.log(McpSchema.LoggingLevel.INFO, "reading item " + id);
            return "item " + id;
        }

        @ResourceCompletion(uri = "catalog://items/{id}")
        List<String> ids(McpRequestContext context) {
            context.log(McpSchema.LoggingLevel.INFO, "completing ids");
            return List.of("42");
        }

        @PromptCompletion(name = "explain")
        List<String> topics(McpRequestContext context) {
            context.log(McpSchema.LoggingLevel.INFO, "completing topics");
            return List.of("mcp");
        }

        @Tool
        String requestInScope(McpRequestContext context) {
            context.log(McpSchema.LoggingLevel.INFO, "checking the request");
            return String.valueOf(ServerRequestContext.currentRequest().isPresent());
        }

        @Prompt
        String explain(String topic, McpRequestContext context, MicronautMcpTransportContext transport, McpSchema.Request request) {
            context.log(McpSchema.LoggingLevel.INFO, "explaining " + topic + " for " + ((McpSchema.GetPromptRequest) request).name());
            return "Explain " + topic;
        }
    }

    record Marker() implements PropagatedContextElement {
    }

    /**
     * Adds a {@link Marker} to the propagated context of each MCP request.
     */
    @Requires(property = PROPERTY, value = StringUtils.TRUE)
    @Filter("/mcp")
    static class MarkerFilter implements HttpServerFilter {
        @Override
        public Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain) {
            try (PropagatedContext.Scope ignore = PropagatedContext.getOrEmpty().plus(new Marker()).propagate()) {
                return chain.proceed(request);
            }
        }
    }
}
