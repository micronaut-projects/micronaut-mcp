package io.micronaut.mcp.client.langchain4j.multi;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
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
import io.micronaut.http.annotation.ClientFilter;
import io.micronaut.http.annotation.RequestFilter;
import io.micronaut.http.annotation.ResponseFilter;
import io.micronaut.http.annotation.ServerFilter;
import io.micronaut.http.context.ServerRequestContext;
import io.micronaut.mcp.annotations.Tool;
import io.micronaut.mcp.client.langchain4j.http.MicronautHttpClientTransports;
import io.micronaut.mcp.conf.client.McpClientHttpConfiguration;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.support.TestPropertyProvider;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Property(name = "micronaut.mcp.server.info.name", value = "http-server")
@Property(name = "micronaut.mcp.server.info.version", value = "1.0.0")
@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "spec.name", value = "LangChain4jMicronautHttpClientTransportTest")
@MicronautTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MicronautHttpClientTransportTest implements TestPropertyProvider {

    @Inject
    @Named("micronaut")
    McpClient client;

    @Inject
    @Named("micronaut")
    McpClientHttpConfiguration configuration;

    @Inject
    MicronautHttpClientTransports transports;

    @Override
    public Map<String, String> getProperties() {
        int port = SocketUtils.findAvailableTcpPort();
        return Map.of(
            "micronaut.server.port", String.valueOf(port),
            "micronaut.mcp.client.http.micronaut.url", "http://localhost:" + port + "/mcp",
            "micronaut.mcp.client.http.micronaut.http-client", "MICRONAUT");
    }

    @Test
    void toolsAreListedAndCalledThroughTheMicronautHttpClient() {
        assertTrue(client.listTools().stream().anyMatch(t -> t.name().equals("filtered")));
        String result = client.executeTool(ToolExecutionRequest.builder().name("filtered").arguments("{}").build()).resultText();
        assertEquals("from the client filter", result);
    }

    @Test
    void protocolErrorsReachTheClient() {
        ToolExecutionRequest request = ToolExecutionRequest.builder().name("protocolError").arguments("{}").build();
        RuntimeException error = assertThrows(RuntimeException.class, () -> client.executeTool(request));
        assertTrue(String.valueOf(error.getMessage()).contains("bad"), String.valueOf(error));
    }

    @Test
    void theMethodOfARequestIsSentInAHeaderWithoutSessionWithThe20260728Protocol() {
        // The client detects the protocol of the server, which falls back to an earlier one, with a request of the
        // 2026-07-28 protocol
        assertTrue(client.listTools().stream().anyMatch(t -> t.name().equals("filtered")));
        assertTrue(Recorder.MODERN_REQUESTS.contains("server/discover null null"), String.valueOf(Recorder.MODERN_REQUESTS));
    }

    @Test
    void aSessionTheServerNoLongerKnowsIsInitializedAgain() throws Exception {
        try (McpClient legacy = new DefaultMcpClient.Builder()
            .key("legacy")
            .protocolVersion("2025-11-25")
            .transport(transports.create(configuration))
            .build()) {
            Recorder.FORGET_SESSION.set(true);
            String result = legacy.executeTool(ToolExecutionRequest.builder().name("filtered").arguments("{}").build()).resultText();
            assertEquals("from the client filter", result);
            assertFalse(Recorder.FORGET_SESSION.get(), "The server answered that it no longer knows the session");
            assertTrue(Recorder.INITIALIZE_WITH_SESSION.isEmpty(), "Initialize is sent without a session id");
        }
    }

    @Requires(property = "spec.name", value = "LangChain4jMicronautHttpClientTransportTest")
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
    }

    /**
     * Records the requests, and acts as a server with sessions, which this stateless server is not.
     */
    @Requires(property = "spec.name", value = "LangChain4jMicronautHttpClientTransportTest")
    @ServerFilter("/mcp")
    static class Recorder {
        static final AtomicBoolean FORGET_SESSION = new AtomicBoolean();
        static final List<String> INITIALIZE_WITH_SESSION = new CopyOnWriteArrayList<>();
        static final List<String> MODERN_REQUESTS = new CopyOnWriteArrayList<>();
        static final AtomicInteger SESSIONS = new AtomicInteger();
        private static final String INITIALIZE = "initialize";

        @RequestFilter
        @Nullable
        HttpResponse<?> filter(HttpRequest<?> request, @Body @Nullable String body) {
            String session = request.getHeaders().get("Mcp-Session-Id");
            String method = request.getHeaders().get("Mcp-Method");
            if (method != null) {
                MODERN_REQUESTS.add(method + " " + request.getHeaders().get("Mcp-Name") + " " + session);
            }
            boolean initialize = body != null && body.contains("\"method\":\"initialize\"");
            if (initialize) {
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

    @Requires(property = "spec.name", value = "LangChain4jMicronautHttpClientTransportTest")
    @ClientFilter("/mcp")
    static class AddHeader {
        @RequestFilter
        void filter(MutableHttpRequest<?> request) {
            request.header("X-Filter", "from the client filter");
        }
    }
}
