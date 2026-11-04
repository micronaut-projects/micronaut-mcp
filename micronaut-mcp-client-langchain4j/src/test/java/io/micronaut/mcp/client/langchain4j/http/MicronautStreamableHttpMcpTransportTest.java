package io.micronaut.mcp.client.langchain4j.http;

import dev.langchain4j.mcp.client.McpCallContext;
import dev.langchain4j.mcp.client.McpException;
import dev.langchain4j.mcp.client.transport.McpOperationHandler;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.StreamableHttpMcpTransport;
import dev.langchain4j.mcp.protocol.McpCallToolRequest;
import dev.langchain4j.mcp.protocol.McpGetPromptRequest;
import dev.langchain4j.mcp.protocol.McpInitializationNotification;
import dev.langchain4j.mcp.protocol.McpInitializeRequest;
import dev.langchain4j.mcp.protocol.McpPingRequest;
import dev.langchain4j.mcp.protocol.McpReadResourceRequest;
import io.micronaut.context.BeanContext;
import io.micronaut.core.io.socket.SocketUtils;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.http.client.exceptions.ReadTimeoutException;
import io.micronaut.inject.qualifiers.Qualifiers;
import io.micronaut.mcp.client.http.McpHttpSessionNotFoundException;
import io.micronaut.mcp.client.langchain4j.http.CraftedMcpServer.Request;
import io.micronaut.mcp.conf.client.McpClientHttpConfiguration;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.support.TestPropertyProvider;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static io.micronaut.mcp.client.langchain4j.http.CraftedMcpServer.SESSION_ID;
import static io.micronaut.mcp.client.langchain4j.http.CraftedMcpServer.body;
import static io.micronaut.mcp.client.langchain4j.http.CraftedMcpServer.delayed;
import static io.micronaut.mcp.client.langchain4j.http.CraftedMcpServer.events;
import static io.micronaut.mcp.client.langchain4j.http.CraftedMcpServer.json;
import static io.micronaut.mcp.client.langchain4j.http.CraftedMcpServer.utf8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the LangChain4j transport over the Micronaut HTTP client against a server answering with crafted responses.
 */
@MicronautTest(startApplication = false)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MicronautStreamableHttpMcpTransportTest implements TestPropertyProvider {
    private static final String MODERN_PROTOCOL = "2026-07-28";
    private static final String INITIALIZE_RESULT = """
        {"jsonrpc":"2.0","id":1,"result":{"protocolVersion":"2025-11-25","capabilities":{},"serverInfo":{"name":"crafted","version":"1"}}}""";

    private final CraftedMcpServer server = CraftedMcpServer.start();

    @Inject
    BeanContext beanContext;

    @Inject
    MicronautHttpClientTransports transports;

    @Override
    public Map<String, String> getProperties() {
        String url = "http://localhost:" + server.port() + "/mcp";
        return Map.of(
            "micronaut.mcp.client.http.crafted.url", url,
            "micronaut.mcp.client.http.crafted.http-client", "MICRONAUT",
            "micronaut.mcp.client.http.crafted.headers.X-Api-Key", "secret",
            "micronaut.mcp.client.http.impatient.url", url,
            "micronaut.mcp.client.http.impatient.http-client", "MICRONAUT",
            "micronaut.mcp.client.http.impatient.request-timeout", "1s",
            "micronaut.mcp.client.http.unreachable.url", "http://localhost:" + SocketUtils.findAvailableTcpPort() + "/mcp",
            "micronaut.mcp.client.http.unreachable.http-client", "MICRONAUT");
    }

    @BeforeEach
    void reset() {
        server.reset();
    }

    @AfterAll
    void stop() {
        server.close();
    }

    @Test
    void aTransportThatIsNotStartedFailsTheRequests() {
        McpTransport transport = transports.create(configuration("crafted"));
        CompletableFuture<String> response = transport.sendRequest(new McpPingRequest(1L));
        assertInstanceOf(IllegalStateException.class, failure(response));
        assertTrue(server.requests("POST").isEmpty());
    }

    @Test
    void requestsAndNotificationsAreSentThroughEachEntryPoint() throws Exception {
        McpTransport transport = transport("crafted");
        server.on("POST ping", json(200, "{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{}}"));

        String response = transport.sendRequest(new McpPingRequest(1L)).get(10, TimeUnit.SECONDS);
        transport.sendMessage(new McpInitializationNotification());
        transport.sendMessage(new McpCallContext(null, new McpInitializationNotification()));

        assertTrue(response.contains("\"result\""), response);
        List<Request> notifications = server.await(r -> r.body().contains("notifications/initialized"), 2);
        assertEquals("secret", notifications.getFirst().header("X-Api-Key"));
    }

    @Test
    void aResponseThatDoesNotAnswerTheRequestFailsIt() {
        McpTransport transport = transport("crafted");
        server.on("POST ping", events(utf8("data: {\"jsonrpc\":\"2.0\",\"method\":\"notifications/message\",\"params\":{\"level\":\"info\",\"data\":\"working\"}}\n\n")));

        Throwable error = failure(transport.sendRequest(new McpPingRequest(1L)));

        assertInstanceOf(IllegalStateException.class, error);
        assertTrue(error.getMessage().contains("did not answer"), error.getMessage());
    }

    @Test
    void theMethodAndTheNameOfARequestAreSentInHeadersWithoutSessionWithThe20260728Protocol() throws Exception {
        McpTransport transport = transport("crafted");
        server.on("POST initialize", json(200, INITIALIZE_RESULT, SESSION_ID, "s1"));
        transport.sendInitializeRequest(new McpInitializeRequest(1L)).get(10, TimeUnit.SECONDS);
        transport.setProtocolVersion(MODERN_PROTOCOL);
        transport.setModernProtocol(true);
        // A session id is ignored without sessions
        server.on("POST tools/call", json(200, "{\"jsonrpc\":\"2.0\",\"id\":2,\"result\":{\"content\":[]}}", SESSION_ID, "s2"));
        server.on("POST resources/read", json(200, "{\"jsonrpc\":\"2.0\",\"id\":3,\"result\":{\"contents\":[]}}"));
        server.on("POST prompts/get", json(200, "{\"jsonrpc\":\"2.0\",\"id\":4,\"result\":{\"messages\":[]}}"));
        server.on("POST ping", json(200, "{\"jsonrpc\":\"2.0\",\"id\":5,\"result\":{}}"));

        transport.sendRequest(new McpCallContext(null, new McpCallToolRequest(2L, "weather", Map.of("city", "Paris")), Map.of("Mcp-Param-City", "Paris")))
            .get(10, TimeUnit.SECONDS);
        transport.sendRequest(new McpReadResourceRequest(3L, "file:///forecast")).get(10, TimeUnit.SECONDS);
        transport.sendRequest(new McpGetPromptRequest(4L, "summary", Map.of())).get(10, TimeUnit.SECONDS);
        transport.sendRequest(new McpPingRequest(5L)).get(10, TimeUnit.SECONDS);

        List<Request> requests = server.requests("POST");
        Request call = requests.get(requests.size() - 4);
        assertEquals("tools/call", call.header("Mcp-Method"));
        assertEquals("weather", call.header("Mcp-Name"));
        assertEquals("Paris", call.header("Mcp-Param-City"));
        assertEquals(MODERN_PROTOCOL, call.header("MCP-Protocol-Version"));
        assertNull(call.header(SESSION_ID));
        Request read = requests.get(requests.size() - 3);
        assertEquals("resources/read", read.header("Mcp-Method"));
        assertEquals("file:///forecast", read.header("Mcp-Name"));
        assertNull(read.header(SESSION_ID));
        assertEquals("summary", requests.get(requests.size() - 2).header("Mcp-Name"));
        Request ping = requests.getLast();
        assertEquals("ping", ping.header("Mcp-Method"));
        assertNull(ping.header("Mcp-Name"));
    }

    @Test
    void errorsTheServerCannotRelateToTheRequestFailIt() throws Exception {
        McpTransport transport = transport("crafted");
        server.on("POST ping", json(200, "{\"jsonrpc\":\"2.0\",\"id\":null,\"error\":{\"code\":-32600,\"message\":\"Invalid session\"}}"));
        server.on("POST ping", json(200, "{\"jsonrpc\":\"2.0\",\"id\":\"other\",\"error\":{\"code\":-32600,\"message\":\"Not this request\"}}"));
        server.on("POST ping", json(200, "{\"jsonrpc\":\"2.0\",\"id\":99,\"error\":{\"code\":-32600,\"message\":\"Another request\"}}"));
        server.on("POST ping", json(400, "{\"jsonrpc\":\"2.0\",\"id\":4,\"error\":{\"code\":-32602,\"message\":\"Bad ping\"}}"));
        server.on("POST ping", json(200, "{\"jsonrpc\":\"2.0\",\"id\":5,\"result\":{\"error\":\"none here\"}}"));

        assertEquals("Invalid session", assertInstanceOf(McpException.class, failure(transport.sendRequest(new McpPingRequest(1L)))).errorMessage());
        assertEquals("Not this request", assertInstanceOf(McpException.class, failure(transport.sendRequest(new McpPingRequest(2L)))).errorMessage());
        assertEquals("Another request", assertInstanceOf(McpException.class, failure(transport.sendRequest(new McpPingRequest(3L)))).errorMessage());
        // The errors and results of the request reach the client
        assertTrue(transport.sendRequest(new McpPingRequest(4L)).get(10, TimeUnit.SECONDS).contains("Bad ping"));
        assertTrue(transport.sendRequest(new McpPingRequest(5L)).get(10, TimeUnit.SECONDS).contains("here"));
    }

    @Test
    void aFailedReinitializationFailsTheRequest() throws Exception {
        McpTransport transport = transport("crafted");
        server.on("POST initialize", json(200, INITIALIZE_RESULT, SESSION_ID, "s1"));
        server.on("POST ping", json(404, ""));
        server.on("POST initialize", body(500, "text/plain", "unavailable"));
        transport.sendInitializeRequest(new McpInitializeRequest(1L)).get(10, TimeUnit.SECONDS);

        Throwable error = failure(transport.sendRequest(new McpPingRequest(2L)));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, assertInstanceOf(HttpClientResponseException.class, error).getStatus());
    }

    @Test
    void aRequestIsSentAgainOnlyOnceInANewSession() throws Exception {
        McpTransport transport = transport("crafted");
        AtomicInteger failures = new AtomicInteger();
        transport.onFailure(failures::incrementAndGet);
        server.on("POST initialize", json(200, INITIALIZE_RESULT, SESSION_ID, "s1"));
        server.on("POST ping", json(404, ""));
        server.on("POST initialize", json(200, INITIALIZE_RESULT, SESSION_ID, "s2"));
        server.on("POST ping", json(404, ""));
        transport.sendInitializeRequest(new McpInitializeRequest(1L)).get(10, TimeUnit.SECONDS);

        Throwable error = failure(transport.sendRequest(new McpPingRequest(2L)));

        assertEquals("s2", assertInstanceOf(McpHttpSessionNotFoundException.class, error).getSessionId());
        assertEquals(0, failures.get(), "The server was reached");
    }

    @Test
    void aSessionTheServerNoLongerKnowsFailsTheRequestBeforeInitialization() {
        McpTransport transport = transport("crafted");
        server.on("POST ping", json(200, "{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{}}", SESSION_ID, "s1"));
        server.on("POST ping", json(404, ""));
        transport.sendRequest(new McpPingRequest(1L)).join();

        Throwable error = failure(transport.sendRequest(new McpPingRequest(2L)));

        assertEquals("s1", assertInstanceOf(McpHttpSessionNotFoundException.class, error).getSessionId());
    }

    @Test
    void onlyAServerThatCannotBeReachedIsAFailureOfTheTransport() {
        AtomicInteger failures = new AtomicInteger();
        McpTransport crafted = transport("crafted");
        crafted.onFailure(failures::incrementAndGet);
        server.on("POST ping", body(500, "text/plain", "boom"));
        assertInstanceOf(HttpClientResponseException.class, failure(crafted.sendRequest(new McpPingRequest(1L))));

        McpTransport impatient = transport("impatient");
        impatient.onFailure(failures::incrementAndGet);
        server.on("POST ping", delayed(Duration.ofSeconds(3), json(200, "{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{}}")));
        assertInstanceOf(ReadTimeoutException.class, failure(impatient.sendRequest(new McpPingRequest(1L))));
        assertEquals(0, failures.get());

        McpTransport unreachable = transport("unreachable");
        unreachable.onFailure(failures::incrementAndGet);
        failure(unreachable.sendRequest(new McpPingRequest(1L)));
        assertEquals(1, failures.get());
    }

    @Test
    void closingOnAnEventLoopEndsTheSessionWithoutBlocking() throws Exception {
        McpTransport transport = transport("crafted");
        server.on("POST initialize", json(200, INITIALIZE_RESULT, SESSION_ID, "s1"));
        transport.sendInitializeRequest(new McpInitializeRequest(1L)).get(10, TimeUnit.SECONDS);

        Mono.fromRunnable(() -> {
            assertTrue(Schedulers.isInNonBlockingThread());
            close(transport);
        }).subscribeOn(Schedulers.parallel()).block(Duration.ofSeconds(10));

        Request delete = server.await("DELETE", 1).getFirst();
        assertEquals("s1", delete.header(SESSION_ID));
        assertEquals("secret", delete.header("X-Api-Key"));
    }

    @Test
    void theConnectionsOfTheMicronautHttpClientHaveNoLangChain4jTransport() {
        assertTrue(beanContext.findBean(StreamableHttpMcpTransport.Builder.class, Qualifiers.byName("crafted")).isEmpty());
    }

    private McpTransport transport(String connection) {
        McpTransport transport = transports.create(configuration(connection));
        transport.start(new McpOperationHandler(new ConcurrentHashMap<>(), List::of, transport, log -> { }, () -> { }, () -> { },
            () -> { }, uri -> { }, null, () -> { }, () -> { }, (id, message) -> { }, (id, params) -> { }));
        return transport;
    }

    private McpClientHttpConfiguration configuration(String connection) {
        return beanContext.getBean(McpClientHttpConfiguration.class, Qualifiers.byName(connection));
    }

    private static Throwable failure(CompletableFuture<String> response) {
        return assertThrows(ExecutionException.class, () -> response.get(10, TimeUnit.SECONDS)).getCause();
    }

    private static void close(McpTransport transport) {
        try {
            transport.close();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
