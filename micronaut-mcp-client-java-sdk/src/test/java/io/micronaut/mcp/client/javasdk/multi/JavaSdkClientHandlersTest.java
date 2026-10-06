package io.micronaut.mcp.client.javasdk.multi;

import io.micronaut.context.BeanContext;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.inject.qualifiers.Qualifiers;
import io.micronaut.mcp.annotations.Tool;
import io.micronaut.mcp.client.javasdk.McpElicitationHandler;
import io.micronaut.mcp.client.javasdk.McpListChangedListener;
import io.micronaut.mcp.client.javasdk.McpLoggingHandler;
import io.micronaut.mcp.client.javasdk.McpProgressHandler;
import io.micronaut.mcp.client.javasdk.McpSamplingHandler;
import io.micronaut.mcp.server.context.McpRequestContext;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.support.TestPropertyProvider;
import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The asynchronous client, and a synchronous one, of HTTP connections with headers, timeouts and every kind of handler.
 */
@Property(name = "micronaut.mcp.server.info.name", value = "http-server")
@Property(name = "micronaut.mcp.server.info.version", value = "1.0.0")
@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "moon.enabled", value = "false")
@Property(name = "spec.name", value = "JavaSdkClientHandlersTest")
@MicronautTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JavaSdkClientHandlersTest implements TestPropertyProvider {

    @Inject
    BeanContext beanContext;

    @Inject
    Notifications notifications;

    @Override
    public Map<String, String> getProperties() {
        int port = freePort();
        String url = "http://localhost:" + port + "/mcp";
        return Map.ofEntries(
            Map.entry("micronaut.server.port", String.valueOf(port)),
            Map.entry("micronaut.mcp.client.http.async.url", url),
            Map.entry("micronaut.mcp.client.http.async.headers.X-Client", "async"),
            Map.entry("micronaut.mcp.client.http.async.request-timeout", "30s"),
            Map.entry("micronaut.mcp.client.http.async.initialization-timeout", "20s"),
            Map.entry("micronaut.mcp.client.http.sync.url", url),
            Map.entry("micronaut.mcp.client.http.sync.initialization-timeout", "20s"));
    }

    @Test
    void theAsynchronousClientCallsTheHandlers() {
        McpAsyncClient client = beanContext.getBean(McpAsyncClient.class, Qualifiers.byName("async"));
        assertNotNull(client.getClientCapabilities().sampling());
        assertNotNull(client.getClientCapabilities().elicitation());
        McpSchema.CallToolResult result = client.callTool(new McpSchema.CallToolRequest("importCatalog", Map.of("pages", 2),
            Map.of("progressToken", "async-1"))).block(Duration.ofSeconds(30));
        assertNotNull(result);
        assertEquals("done", ((McpSchema.TextContent) result.content().getFirst()).text());
        assertEquals(Set.of("async 1.0/2.0", "async 2.0/2.0"), await(notifications.progress, "async", 2));
        assertEquals(Set.of("async imported 2 pages"), await(notifications.logs, "async", 1));
    }

    @Test
    void theSynchronousClientAdvertisesItsHandlers() {
        McpSyncClient client = beanContext.getBean(McpSyncClient.class, Qualifiers.byName("sync"));
        assertTrue(client.isInitialized());
        assertNotNull(client.getClientCapabilities().elicitation());
        assertEquals("pong", ((McpSchema.TextContent) client.callTool(new McpSchema.CallToolRequest("ping", Map.of()))
            .content().getFirst()).text());
    }

    @Test
    void listChangedListenersOnlyImplementWhatTheyNeed() {
        McpListChangedListener listener = new McpListChangedListener() { };
        listener.toolsChanged("c", List.of());
        listener.promptsChanged("c", List.of());
        listener.resourcesChanged("c", List.of());
        assertTrue(notifications.progress.stream().noneMatch(p -> p.startsWith("c ")));
    }

    private static Set<String> await(List<String> received, String client, int size) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (received.stream().filter(r -> r.startsWith(client + " ")).count() < size && System.nanoTime() < deadline) {
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(20));
        }
        return Set.copyOf(received.stream().filter(r -> r.startsWith(client + " ")).toList());
    }

    private static int freePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Requires(property = "spec.name", value = "JavaSdkClientHandlersTest")
    @Singleton
    static class ServerTools {
        @Tool(description = "Imports a catalog")
        String importCatalog(int pages, McpRequestContext context) {
            for (int page = 1; page <= pages; page++) {
                context.progress(page, (double) pages, null);
            }
            context.log(McpSchema.LoggingLevel.INFO, "catalog", "imported " + pages + " pages");
            return "done";
        }

        @Tool(description = "Answers pong")
        String ping() {
            return "pong";
        }
    }

    @Requires(property = "spec.name", value = "JavaSdkClientHandlersTest")
    @Singleton
    static class Notifications implements McpProgressHandler, McpLoggingHandler, McpSamplingHandler, McpElicitationHandler,
        McpListChangedListener {
        final List<String> progress = new CopyOnWriteArrayList<>();
        final List<String> logs = new CopyOnWriteArrayList<>();

        @Override
        public void progress(String client, McpSchema.ProgressNotification notification) {
            progress.add(client + " " + notification.progress() + "/" + notification.total());
        }

        @Override
        public void log(String client, McpSchema.LoggingMessageNotification notification) {
            logs.add(client + " " + notification.data());
        }

        @Override
        public McpSchema.CreateMessageResult sample(String client, McpSchema.CreateMessageRequest request) {
            return McpSchema.CreateMessageResult.builder().content(new McpSchema.TextContent("sampled")).model("test").build();
        }

        @Override
        public McpSchema.ElicitResult elicit(String client, McpSchema.ElicitFormRequest request) {
            return new McpSchema.ElicitResult(McpSchema.ElicitResult.Action.DECLINE, null);
        }
    }
}
