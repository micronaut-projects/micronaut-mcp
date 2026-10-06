package io.micronaut.mcp.client.javasdk.multi;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.io.socket.SocketUtils;
import io.micronaut.http.HttpMethod;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.ResponseFilter;
import io.micronaut.http.annotation.ServerFilter;
import io.micronaut.http.annotation.ClientFilter;
import io.micronaut.http.annotation.RequestFilter;
import io.micronaut.http.context.ServerRequestContext;
import io.micronaut.mcp.annotations.Tool;
import io.micronaut.mcp.client.javasdk.McpProgressHandler;
import io.micronaut.mcp.server.context.McpRequestContext;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.support.TestPropertyProvider;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpTransportSessionNotFoundException;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Property(name = "micronaut.mcp.server.info.name", value = "http-server")
@Property(name = "micronaut.mcp.server.info.version", value = "1.0.0")
@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "moon.enabled", value = "false")
@Property(name = "spec.name", value = "MicronautHttpClientTransportTest")
@MicronautTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MicronautHttpClientTransportTest implements TestPropertyProvider {
    static final CountDownLatch CLIENT_GOT_PROGRESS = new CountDownLatch(1);
    static final String UNICODE = "h\u00e9llo w\u00f6rld \u20ac \uD83D\uDE00 ".repeat(20_000);

    @Inject
    @Named("micronaut")
    McpSyncClient client;

    @Inject
    @Named("small")
    McpSyncClient smallClient;

    @Override
    public Map<String, String> getProperties() {
        int port = SocketUtils.findAvailableTcpPort();
        return Map.of(
            "micronaut.server.port", String.valueOf(port),
            "micronaut.mcp.client.http.micronaut.url", "http://localhost:" + port + "/mcp",
            "micronaut.mcp.client.http.micronaut.http-client", "MICRONAUT",
            "micronaut.mcp.client.http.micronaut.request-timeout", "30s",
            "micronaut.mcp.client.http.small.url", "http://localhost:" + port + "/mcp",
            "micronaut.mcp.client.http.small.http-client", "MICRONAUT",
            "micronaut.mcp.client.http.small.max-message-size", "4KB");
    }

    @Test
    void toolsAreListedAndCalledThroughTheMicronautHttpClient() {
        client.initialize();
        assertTrue(client.listTools().tools().stream().anyMatch(t -> t.name().equals("filtered")));
        McpSchema.CallToolResult result = client.callTool(new McpSchema.CallToolRequest("filtered", Map.of()));
        assertEquals("from the client filter", text(result));
    }

    @Test
    void jsonRpcErrorsAnsweredWithAnErrorStatusReachTheClient() {
        client.initialize();
        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("protocolError", Map.of());
        McpError error = assertThrows(McpError.class, () -> client.callTool(request));
        assertEquals(McpSchema.ErrorCodes.INVALID_PARAMS, error.getJsonRpcError().code());
    }

    @Test
    void eventsOfAStreamedResponseArriveWhileTheToolRuns() {
        client.initialize();
        McpSchema.CallToolResult result = client.callTool(new McpSchema.CallToolRequest("waitForClient", Map.of(), Map.of("progressToken", "p")));
        assertEquals("the client saw the progress", text(result));
    }

    @Test
    void charactersSplitAcrossTheChunksOfAStreamAreDecoded() {
        McpSchema.CallToolResult result = client.callTool(new McpSchema.CallToolRequest("unicode", Map.of(), Map.of("progressToken", "u")));
        assertEquals(UNICODE, text(result));
    }

    @Test
    void messagesLargerThanTheMaximumSizeFail() {
        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("unicode", Map.of());
        RuntimeException error = assertThrows(RuntimeException.class, () -> smallClient.callTool(request));
        assertTrue(String.valueOf(McpError.findRootCause(error).getMessage()).contains("max-message-size"), String.valueOf(error));
    }

    @Test
    void theRequestTimeoutOfTheConnectionIsTheReadTimeout() {
        // Longer than the default read timeout of the Micronaut HTTP client
        assertEquals("slow", text(client.callTool(new McpSchema.CallToolRequest("slow", Map.of()))));
    }

    @Test
    void aSessionTheServerNoLongerKnowsIsInitializedAgain() {
        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("filtered", Map.of());
        Sessions.FORGET_SESSION.set(true);
        try {
            client.callTool(request);
        } catch (RuntimeException e) {
            // The client may fail the request that found the session gone
            assertTrue(e instanceof McpTransportSessionNotFoundException || e.getCause() instanceof McpTransportSessionNotFoundException, String.valueOf(e));
        }
        assertFalse(Sessions.FORGET_SESSION.get(), "The server answered that it no longer knows the session");
        assertEquals("from the client filter", text(client.callTool(request)));
        assertTrue(Sessions.INITIALIZE_WITH_SESSION.isEmpty(), "Initialize is sent without a session id");
    }

    private static String text(McpSchema.CallToolResult result) {
        return ((McpSchema.TextContent) result.content().getFirst()).text();
    }

    @Requires(property = "spec.name", value = "MicronautHttpClientTransportTest")
    @Singleton
    static class Tools {
        @Tool
        String filtered() {
            return ServerRequestContext.currentRequest().map(r -> r.getHeaders().get("X-Filter")).orElse("no request");
        }

        @Tool
        String protocolError() {
            throw McpError.builder(McpSchema.ErrorCodes.INVALID_PARAMS).message("bad").build();
        }

        @Tool
        String unicode(McpRequestContext context) {
            // Streamed, after the progress
            context.progress(1);
            return UNICODE;
        }

        @Tool
        String slow() {
            // Longer than the default read timeout of the HTTP client
            LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(11));
            return "slow";
        }

        @Tool
        String waitForClient(McpRequestContext context) throws InterruptedException {
            context.progress(1);
            return CLIENT_GOT_PROGRESS.await(10, TimeUnit.SECONDS) ? "the client saw the progress" : "the progress was not streamed";
        }
    }

    @Requires(property = "spec.name", value = "MicronautHttpClientTransportTest")
    @Singleton
    static class Progress implements McpProgressHandler {
        @Override
        public void progress(String client, McpSchema.ProgressNotification notification) {
            CLIENT_GOT_PROGRESS.countDown();
        }
    }

    /**
     * Acts as a server with sessions, which this stateless server is not.
     */
    @Requires(property = "spec.name", value = "MicronautHttpClientTransportTest")
    @ServerFilter("/mcp")
    static class Sessions {
        static final AtomicBoolean FORGET_SESSION = new AtomicBoolean();
        static final List<String> INITIALIZE_WITH_SESSION = new CopyOnWriteArrayList<>();
        static final AtomicInteger SESSIONS = new AtomicInteger();
        private static final String INITIALIZE = "initialize";

        @RequestFilter
        @Nullable
        HttpResponse<?> filter(HttpRequest<?> request, @Body @Nullable String body) {
            String session = request.getHeaders().get("Mcp-Session-Id");
            if (body != null && body.contains("\"method\":\"initialize\"")) {
                request.setAttribute(INITIALIZE, true);
                if (session != null) {
                    INITIALIZE_WITH_SESSION.add(session);
                }
            }
            if (session != null && request.getMethod() == HttpMethod.POST && FORGET_SESSION.compareAndSet(true, false)) {
                return HttpResponse.notFound("{\"jsonrpc\":\"2.0\",\"id\":null,\"error\":{\"code\":-32001,\"message\":\"Session not found\"}}")
                    .contentType(MediaType.APPLICATION_JSON_TYPE);
            }
            return null;
        }

        @ResponseFilter
        void session(HttpRequest<?> request, MutableHttpResponse<?> response) {
            if (request.getMethod() == HttpMethod.POST && request.getAttribute(INITIALIZE).isPresent()) {
                response.header("Mcp-Session-Id", "session-" + SESSIONS.incrementAndGet());
            }
        }
    }

    @Requires(property = "spec.name", value = "MicronautHttpClientTransportTest")
    @ClientFilter("/mcp")
    static class AddHeader {
        @RequestFilter
        void filter(MutableHttpRequest<?> request) {
            request.header("X-Filter", "from the client filter");
        }
    }
}
