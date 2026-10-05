package io.micronaut.mcp.server.stateless;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.mcp.annotations.Prompt;
import io.micronaut.mcp.annotations.Tool;
import io.micronaut.mcp.server.context.McpRequestContext;
import io.micronaut.mcp.server.context.MicronautMcpTransportContext;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Property(name = "micronaut.mcp.server.info.name", value = "mcp-server")
@Property(name = "micronaut.mcp.server.info.version", value = "0.0.1")
@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "spec.name", value = "RequestContextStreamingTest")
@MicronautTest
class RequestContextStreamingTest {
    private static final String ACCEPT_BOTH = "application/json, text/event-stream";

    @Inject
    @Client("/")
    HttpClient httpClient;

    @Inject
    EmbeddedServer server;

    @Test
    void notificationsUpgradeTheResponseToAStream() {
        HttpResponse<String> response = post(ACCEPT_BOTH, """
            {"jsonrpc": "2.0", "id": 7, "method": "tools/call",
             "params": {"name": "importCatalog", "arguments": {"pages": 2}, "_meta": {"progressToken": "p1"}}}""");
        assertTrue(response.getContentType().map(MediaType.TEXT_EVENT_STREAM_TYPE::matches).orElse(false), response.getContentType().toString());
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
    void theRequestContextIsNotPartOfTheInputSchemaOrPromptArguments() {
        String tools = post(MediaType.APPLICATION_JSON, """
            {"jsonrpc": "2.0", "id": 11, "method": "tools/list", "params": {}}""").body();
        assertTrue(tools.contains("\"pages\""), tools);
        assertFalse(tools.contains("context"), tools);
        String prompts = post(MediaType.APPLICATION_JSON, """
            {"jsonrpc": "2.0", "id": 12, "method": "prompts/list", "params": {}}""").body();
        assertTrue(prompts.contains("\"arguments\":[{\"name\":\"topic\""), prompts);
        assertFalse(prompts.contains("\"name\":\"context\"") || prompts.contains("\"name\":\"transport\""), prompts);
    }

    @Test
    void notificationsReachTheClientWhileTheToolRuns() throws Exception {
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

    @Requires(property = "spec.name", value = "RequestContextStreamingTest")
    @Singleton
    static class Primitives {
        @Tool
        String importCatalog(int pages, McpRequestContext context) {
            for (int page = 1; page <= pages; page++) {
                context.progress(page, (double) pages, "Imported page " + page);
            }
            context.log(McpSchema.LoggingLevel.INFO, "catalog", "imported " + pages + " pages");
            return "done";
        }

        static final CountDownLatch CLIENT_GOT_PROGRESS = new CountDownLatch(1);

        @Tool
        @ExecuteOn(TaskExecutors.BLOCKING)
        String waitForClient(McpRequestContext context) throws InterruptedException {
            context.progress(1);
            return CLIENT_GOT_PROGRESS.await(10, TimeUnit.SECONDS) ? "the client saw the progress" : "the progress was not streamed";
        }

        @Tool
        String quiet(McpRequestContext context) {
            return "quiet";
        }

        @Prompt
        String explain(String topic, McpRequestContext context, MicronautMcpTransportContext transport) {
            return "Explain " + topic;
        }
    }
}
