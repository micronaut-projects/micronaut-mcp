package io.micronaut.mcp.client.javasdk.multi;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.io.socket.SocketUtils;
import io.micronaut.http.MutableHttpRequest;
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
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @Inject
    @Named("micronaut")
    McpSyncClient client;

    @Override
    public Map<String, String> getProperties() {
        int port = SocketUtils.findAvailableTcpPort();
        return Map.of(
            "micronaut.server.port", String.valueOf(port),
            "micronaut.mcp.client.http.micronaut.url", "http://localhost:" + port + "/mcp",
            "micronaut.mcp.client.http.micronaut.http-client", "MICRONAUT",
            "micronaut.mcp.client.http.micronaut.request-timeout", "30s");
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

    @Requires(property = "spec.name", value = "MicronautHttpClientTransportTest")
    @ClientFilter("/mcp")
    static class AddHeader {
        @RequestFilter
        void filter(MutableHttpRequest<?> request) {
            request.header("X-Filter", "from the client filter");
        }
    }
}
