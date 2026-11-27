package io.micronaut.mcp.client.javasdk;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.BeanContext;
import io.micronaut.context.annotation.Property;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.annotation.ClientFilter;
import io.micronaut.http.annotation.RequestFilter;
import io.micronaut.http.client.StreamingHttpClientRegistry;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.http.client.exceptions.ReadTimeoutException;
import io.micronaut.inject.qualifiers.Qualifiers;
import io.micronaut.mcp.client.javasdk.CraftedMcpServer.Request;
import io.micronaut.mcp.conf.client.McpClientHttpConfiguration;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.support.TestPropertyProvider;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.schema.JsonSchemaValidator;
import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpTransportSessionNotFoundException;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static io.micronaut.mcp.client.javasdk.CraftedMcpServer.SESSION_ID;
import static io.micronaut.mcp.client.javasdk.CraftedMcpServer.body;
import static io.micronaut.mcp.client.javasdk.CraftedMcpServer.delayed;
import static io.micronaut.mcp.client.javasdk.CraftedMcpServer.events;
import static io.micronaut.mcp.client.javasdk.CraftedMcpServer.json;
import static io.micronaut.mcp.client.javasdk.CraftedMcpServer.openStream;
import static io.micronaut.mcp.client.javasdk.CraftedMcpServer.status;
import static io.micronaut.mcp.client.javasdk.CraftedMcpServer.utf8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the transport over the Micronaut HTTP client against a server answering with crafted responses.
 */
@Property(name = "moon.enabled", value = "false")
@MicronautTest(startApplication = false)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MicronautStreamableHttpClientTransportTest implements TestPropertyProvider {
    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private static final String PROTOCOL_VERSION = "2025-06-18";
    private static final String INITIALIZE_RESULT = """
        {"jsonrpc":"2.0","id":"1","result":{"protocolVersion":"2025-06-18","capabilities":{},"serverInfo":{"name":"crafted","version":"1"}}}""";

    private final CraftedMcpServer server = CraftedMcpServer.start();

    @Inject
    BeanContext beanContext;

    @Inject
    MicronautHttpClientTransports transports;

    @Override
    public Map<String, String> getProperties() {
        String url = "http://localhost:" + server.port();
        return Map.ofEntries(
            Map.entry("micronaut.mcp.client.http.crafted.url", url + "/mcp?tenant=acme"),
            Map.entry("micronaut.mcp.client.http.crafted.http-client", "MICRONAUT"),
            Map.entry("micronaut.mcp.client.http.crafted.headers.X-Api-Key", "secret"),
            Map.entry("micronaut.mcp.client.http.small.url", url + "/mcp"),
            Map.entry("micronaut.mcp.client.http.small.http-client", "MICRONAUT"),
            Map.entry("micronaut.mcp.client.http.small.max-message-size", "1KB"),
            // Without a path, the endpoint is /mcp, as with the JDK HTTP client
            Map.entry("micronaut.mcp.client.http.root.url", url),
            Map.entry("micronaut.mcp.client.http.root.http-client", "MICRONAUT"),
            Map.entry("micronaut.mcp.client.http.root-query.url", url + "?tenant=acme"),
            Map.entry("micronaut.mcp.client.http.root-query.http-client", "MICRONAUT"),
            // The service names the server, the connection the path of the endpoint
            Map.entry("micronaut.http.services.crafted-service.url", url),
            Map.entry("micronaut.mcp.client.http.service.url", "http://unused.invalid/mcp"),
            Map.entry("micronaut.mcp.client.http.service.http-client", "MICRONAUT"),
            Map.entry("micronaut.mcp.client.http.service.service-id", "crafted-service"),
            // The service configures the client, the connection names the server
            Map.entry("micronaut.http.services.impatient.read-timeout", "1s"),
            Map.entry("micronaut.mcp.client.http.impatient.url", url + "/mcp"),
            Map.entry("micronaut.mcp.client.http.impatient.http-client", "MICRONAUT"),
            Map.entry("micronaut.mcp.client.http.impatient.service-id", "impatient"),
            Map.entry("micronaut.mcp.client.http.unknown-service.url", url + "/mcp"),
            Map.entry("micronaut.mcp.client.http.unknown-service.http-client", "MICRONAUT"),
            Map.entry("micronaut.mcp.client.http.unknown-service.service-id", "unknown"));
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
    void theStandaloneStreamDeliversTheMessagesOfTheServerUntilTheSessionEnds() throws Exception {
        server.on("POST initialize", json(200, INITIALIZE_RESULT, SESSION_ID, "s1"));
        server.on("GET", events(
            utf8(": a comment\r\nevent: message\r\nid: 1\r\n"),
            // An event whose data spans two lines, with and without a space after the colon
            utf8("data: {\"jsonrpc\":\"2.0\",\r\ndata:\"method\":\"notifications/message\",\"params\":{\"level\":\"info\",\"data\":\"h"),
            // A character split across two chunks
            new byte[] {(byte) 0xC3},
            new byte[] {(byte) 0xA9, 'l', 'l', 'o', '"', '}', '}', '\r', '\n', '\r', '\n'},
            // The last event of a stream ending without a blank line
            utf8("data: {\"jsonrpc\":\"2.0\",\"method\":\"notifications/tools/list_changed\"}\n")));
        // The server no longer knows the session when the client reconnects
        server.on("GET", json(404, ""));
        List<McpSchema.JSONRPCMessage> received = new CopyOnWriteArrayList<>();
        McpClientTransport transport = transport("crafted", received);
        CompletableFuture<Throwable> expired = new CompletableFuture<>();
        transport.setExceptionHandler(expired::complete);

        transport.sendMessage(initialize()).block(TIMEOUT);
        transport.sendMessage(initialized()).block(TIMEOUT);

        List<Request> streams = server.await("GET", 2);
        Request stream = streams.getFirst();
        assertNull(stream.header("Last-Event-ID"));
        assertEquals("1", streams.get(1).header("Last-Event-ID"));
        assertInstanceOf(McpTransportSessionNotFoundException.class, expired.get(10, TimeUnit.SECONDS));
        assertEquals("s1", stream.header(SESSION_ID));
        assertEquals(PROTOCOL_VERSION, stream.header("MCP-Protocol-Version"));
        assertEquals("secret", stream.header("X-Api-Key"));
        assertEquals("text/event-stream", stream.header("Accept"));
        assertEquals("tenant=acme", stream.uri().getRawQuery());
        List<McpSchema.JSONRPCNotification> notifications = received.stream()
            .filter(McpSchema.JSONRPCNotification.class::isInstance)
            .map(McpSchema.JSONRPCNotification.class::cast)
            .toList();
        assertEquals(List.of(McpSchema.METHOD_NOTIFICATION_MESSAGE, McpSchema.METHOD_NOTIFICATION_TOOLS_LIST_CHANGED),
            notifications.stream().map(McpSchema.JSONRPCNotification::method).toList());
        assertEquals("héllo", ((Map<?, ?>) notifications.getFirst().params()).get("data"));
        // The session ended: the stream is not opened again, and closing sends no request
        pauseLongerThanTheReconnectDelay();
        assertEquals(2, server.requests("GET").size());
        transport.closeGracefully().block(TIMEOUT);
        assertTrue(server.requests("DELETE").isEmpty());
    }

    @Test
    void closingEndsTheSessionWithTheHeadersOfTheConnection() {
        CountDownLatch release = new CountDownLatch(1);
        server.on("POST initialize", json(200, INITIALIZE_RESULT, SESSION_ID, "s2"));
        server.on("GET", openStream(release));
        McpClientTransport transport = transport("crafted", new CopyOnWriteArrayList<>());
        transport.sendMessage(initialize()).block(TIMEOUT);
        transport.sendMessage(initialized()).block(TIMEOUT);
        server.await("GET", 1);

        transport.closeGracefully().block(TIMEOUT);
        release.countDown();

        Request delete = server.await("DELETE", 1).getFirst();
        assertEquals("s2", delete.header(SESSION_ID));
        assertEquals(PROTOCOL_VERSION, delete.header("MCP-Protocol-Version"));
        assertEquals("secret", delete.header("X-Api-Key"));
        // A closed transport does not open the stream again
        transport.sendMessage(initialized()).block(TIMEOUT);
        pauseLongerThanTheReconnectDelay();
        assertEquals(1, server.requests("GET").size());
    }

    @Test
    void theStandaloneStreamIsOpenedAgainAfterAnErrorUntilTheServerAnswersItOffersNone() {
        server.on("POST initialize", json(200, INITIALIZE_RESULT));
        server.on("GET", body(500, "text/plain", "busy"));
        server.on("GET", status(405));
        McpClientTransport transport = transport("crafted", new CopyOnWriteArrayList<>());
        transport.sendMessage(initialize()).block(TIMEOUT);
        transport.sendMessage(initialized()).block(TIMEOUT);

        server.await("GET", 2);
        pauseLongerThanTheReconnectDelay();
        assertEquals(2, server.requests("GET").size());
        // A new initialization replaces the stream
        transport.sendMessage(initialized()).block(TIMEOUT);
        server.await("GET", 3);
        assertNull(server.requests("GET").getLast().header(SESSION_ID));
        transport.closeGracefully().block(TIMEOUT);
    }

    @Test
    void theLastEventOfAStreamedResponseNeedsNoLineEnd() {
        List<McpSchema.JSONRPCMessage> received = new CopyOnWriteArrayList<>();
        McpClientTransport transport = transport("crafted", received);
        server.on("POST tools/list", events(utf8("event: message\r\n"), utf8("data: {\"jsonrpc\":\"2.0\",\"id\":\"1\",\"result\":{\"tools\":[]}}")));

        transport.sendMessage(toolsList("1")).block(TIMEOUT);

        McpSchema.JSONRPCResponse response = assertInstanceOf(McpSchema.JSONRPCResponse.class, received.getFirst());
        assertEquals("1", response.id());
        assertEquals(Map.of("tools", List.of()), response.result());
    }

    @Test
    void onlyTheInitializationOfTheServerNegotiatesTheProtocolAndOpensTheStream() {
        McpClientTransport transport = transport("crafted", new CopyOnWriteArrayList<>());
        // A result with a protocol version that does not answer initialize
        server.on("POST tools/list", json(200, "{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"result\":{\"protocolVersion\":\"2025-06-18\",\"tools\":[]}}"));

        transport.sendMessage(toolsList("1")).block(TIMEOUT);
        transport.sendMessage(new McpSchema.JSONRPCNotification(McpSchema.METHOD_NOTIFICATION_ROOTS_LIST_CHANGED)).block(TIMEOUT);
        transport.closeGracefully().block(TIMEOUT);

        Request notification = server.await("POST", 2).getLast();
        assertNull(notification.header("MCP-Protocol-Version"));
        assertTrue(server.requests("GET").isEmpty());
        assertTrue(server.requests("DELETE").isEmpty());
    }

    @Test
    void errorsTheServerCannotRelateToTheRequestFailIt() {
        List<McpSchema.JSONRPCMessage> received = new CopyOnWriteArrayList<>();
        McpClientTransport transport = transport("crafted", received);
        server.on("POST tools/list", json(200, """
            {"jsonrpc":"2.0","id":null,"error":{"code":-32600,"message":"Invalid session"}}"""));
        server.on("POST tools/list", json(200, """
            {"jsonrpc":"2.0","id":"other","error":{"code":-32600,"message":"Not this request"}}"""));
        server.on("POST tools/list", json(400, """
            {"jsonrpc":"2.0","id":"7","error":{"code":-32602,"message":"Bad cursor"}}"""));

        Mono<Void> withoutId = transport.sendMessage(toolsList("5"));
        assertEquals("Invalid session", assertThrows(McpError.class, () -> withoutId.block(TIMEOUT)).getMessage());
        Mono<Void> otherId = transport.sendMessage(toolsList("6"));
        assertEquals("Not this request", assertThrows(McpError.class, () -> otherId.block(TIMEOUT)).getMessage());
        // The error of the request, answered with an error status, reaches the client session
        transport.sendMessage(toolsList("7")).block(TIMEOUT);
        McpSchema.JSONRPCResponse response = assertInstanceOf(McpSchema.JSONRPCResponse.class, received.getFirst());
        assertEquals("7", response.id());
        assertEquals("Bad cursor", response.error().message());
    }

    @Test
    void responsesThatAreNotJsonRpcMessagesFailTheRequest() {
        McpClientTransport transport = transport("crafted", new CopyOnWriteArrayList<>());
        server.on("POST tools/list", json(200, "{\"jsonrpc\":"));
        server.on("POST tools/list", body(500, "text/plain", "boom"));
        server.on("POST tools/list", json(503, "{\"message\":\"unavailable\"}"));
        // Without a session, a 404 is not about the session
        server.on("POST tools/list", json(404, "{\"message\":\"not found\"}"));

        Mono<Void> invalidJson = transport.sendMessage(toolsList("1"));
        assertInstanceOf(IOException.class, Exceptions.unwrap(assertThrows(RuntimeException.class, () -> invalidJson.block(TIMEOUT))));
        assertStatus(HttpStatus.INTERNAL_SERVER_ERROR, transport.sendMessage(toolsList("2")));
        assertStatus(HttpStatus.SERVICE_UNAVAILABLE, transport.sendMessage(toolsList("3")));
        assertStatus(HttpStatus.NOT_FOUND, transport.sendMessage(toolsList("4")));
    }

    @Test
    void redirectsAreNotFollowed() {
        McpClientTransport transport = transport("crafted", new CopyOnWriteArrayList<>());
        server.on("POST tools/list", body(302, "text/plain", "Moved", "Location", "http://localhost:" + server.port() + "/elsewhere"));

        assertStatus(HttpStatus.FOUND, transport.sendMessage(toolsList("1")));
        assertEquals(List.of("/mcp"), server.requests("POST").stream().map(request -> request.uri().getPath()).toList());
    }

    @Test
    void messagesLargerThanTheMaximumSizeFail() {
        McpClientTransport transport = transport("small", new CopyOnWriteArrayList<>());
        // An event whose lines are each smaller than the maximum
        server.on("POST tools/list", events(utf8("data: " + "a".repeat(600) + "\n"), utf8("data: " + "b".repeat(600) + "\n\n")));
        // A line not yet ended
        server.on("POST tools/list", events(utf8("data: " + "c".repeat(2000)), utf8("\n\n")));
        // A JSON-RPC error answered with an error status
        server.on("POST tools/list", json(400, "{\"jsonrpc\":\"2.0\",\"id\":\"3\",\"error\":{\"code\":-32602,\"message\":\"" + "d".repeat(2000) + "\"}}"));

        assertTooLarge(transport.sendMessage(toolsList("1")));
        assertTooLarge(transport.sendMessage(toolsList("2")));
        assertStatus(HttpStatus.BAD_REQUEST, transport.sendMessage(toolsList("3")));
    }

    @Test
    void aUrlWithoutPathUsesTheMcpEndpoint() {
        transport("root", new CopyOnWriteArrayList<>()).sendMessage(toolsList("1")).block(TIMEOUT);
        transport("root-query", new CopyOnWriteArrayList<>()).sendMessage(toolsList("2")).block(TIMEOUT);
        List<Request> requests = server.await("POST", 2);
        assertEquals(List.of("/mcp", "/mcp"), requests.stream().map(request -> request.uri().getPath()).toList());
        assertNull(requests.getFirst().uri().getRawQuery());
        assertEquals("tenant=acme", requests.getLast().uri().getRawQuery());
    }

    @Test
    void aStreamClosedBeforeTheResponseIsResumedFromItsLastEvent() {
        List<McpSchema.JSONRPCMessage> received = new CopyOnWriteArrayList<>();
        McpClientTransport transport = transport("crafted", received);
        // A priming event, then the server closes the stream; the response is delivered on the resumed stream
        server.on("POST tools/list", events(utf8("id: e1\nretry: 10\ndata: \n\n")));
        server.on("GET", events(utf8("id: e2\ndata: {\"jsonrpc\":\"2.0\",\"id\":\"1\",\"result\":{\"tools\":[]}}\n\n")));

        transport.sendMessage(toolsList("1")).block(TIMEOUT);

        assertEquals("e1", server.requests("GET").getFirst().header("Last-Event-ID"));
        McpSchema.JSONRPCResponse response = assertInstanceOf(McpSchema.JSONRPCResponse.class, received.getFirst());
        assertEquals("1", response.id());
    }

    @Test
    void theMaximumSizeIsCountedInBytes() {
        McpClientTransport transport = transport("small", new CopyOnWriteArrayList<>());
        // Fewer characters than the maximum, but more bytes, over two lines that are each smaller than the maximum
        server.on("POST tools/list", events(utf8("data: " + "é".repeat(300) + "\n"), utf8("data: " + "é".repeat(300) + "\n\n")));
        server.on("POST tools/list", json(400, "{\"jsonrpc\":\"2.0\",\"id\":\"2\",\"error\":{\"code\":-32602,\"message\":\"" + "é".repeat(600) + "\"}}"));

        assertTooLarge(transport.sendMessage(toolsList("1")));
        assertStatus(HttpStatus.BAD_REQUEST, transport.sendMessage(toolsList("2")));
    }

    @Test
    void anExpiredSessionIsReportedToTheClientSession() {
        McpClientTransport transport = transport("crafted", new CopyOnWriteArrayList<>());
        List<Throwable> errors = new CopyOnWriteArrayList<>();
        transport.setExceptionHandler(errors::add);
        server.on("POST initialize", json(200, INITIALIZE_RESULT, SESSION_ID, "expiring"));
        server.on("POST tools/list", status(404));

        transport.sendMessage(initialize()).block(TIMEOUT);
        Mono<Void> expired = transport.sendMessage(toolsList("2"));
        assertThrows(RuntimeException.class, () -> expired.block(TIMEOUT));

        // The client session initializes a new session when it is told the server no longer knows this one
        assertInstanceOf(McpTransportSessionNotFoundException.class, errors.getFirst());
        transport.closeGracefully().block(TIMEOUT);
    }

    @Test
    void messagesThatCannotBeWrittenFailWithoutARequest() {
        McpClientTransport transport = transport("crafted", new CopyOnWriteArrayList<>());
        Mono<Void> sent = transport.sendMessage(new McpSchema.JSONRPCRequest(McpSchema.METHOD_TOOLS_CALL, "1", Map.of("value", new Object())));
        assertInstanceOf(IOException.class, Exceptions.unwrap(assertThrows(RuntimeException.class, () -> sent.block(TIMEOUT))));
        assertTrue(server.requests("POST").isEmpty());
    }

    @Test
    void aServiceWithAUrlNamesTheServer() {
        McpClientTransport transport = transport("service", new CopyOnWriteArrayList<>());
        server.on("POST initialize", json(200, INITIALIZE_RESULT));
        transport.sendMessage(initialize()).block(TIMEOUT);
        assertEquals("/mcp", server.requests("POST").getFirst().uri().getPath());
    }

    @Test
    void aServiceWithoutUrlConfiguresTheClientOfTheConnection() {
        McpClientTransport transport = transport("impatient", new CopyOnWriteArrayList<>());
        server.on("POST tools/list", delayed(Duration.ofSeconds(3), json(200, INITIALIZE_RESULT)));
        Mono<Void> sent = transport.sendMessage(toolsList("1"));
        assertInstanceOf(ReadTimeoutException.class, Exceptions.unwrap(assertThrows(RuntimeException.class, () -> sent.block(TIMEOUT))));
        assertEquals("impatient", server.requests("POST").getFirst().header("X-Service"));
    }

    @Test
    void anUnknownServiceUsesTheDefaultClientConfiguration() {
        McpClientTransport transport = transport("unknown-service", new CopyOnWriteArrayList<>());
        server.on("POST initialize", json(200, INITIALIZE_RESULT));
        transport.sendMessage(initialize()).block(TIMEOUT);
        assertEquals(1, server.requests("POST").size());
        assertNull(server.requests("POST").getFirst().header("X-Service"));
    }

    @Test
    void theConnectionsOfTheMicronautHttpClientHaveNoJdkTransport() {
        assertTrue(beanContext.findBean(HttpClientStreamableHttpTransport.Builder.class, Qualifiers.byName("crafted")).isEmpty());
    }

    @Test
    void theJdkClientFactoryWorksWithoutAStreamingRegistry() {
        try (ApplicationContext context = ApplicationContext.run(Map.of("micronaut.http.client.netty.enabled", false))) {
            assertTrue(context.findBean(StreamingHttpClientRegistry.class).isEmpty());
            assertTrue(context.findBean(MicronautHttpClientTransports.class).isEmpty());
            assertTrue(context.findBean(McpClientFactory.class).isPresent());
        }
    }

    @Test
    void theConnectionsOfTheMicronautHttpClientNeedTheMicronautHttpClient() {
        McpClientFactory factory = new McpClientFactory(beanContext.getBean(McpJsonMapper.class), beanContext, null, null,
            List.of(), List.of(), List.of(), List.of(), null, beanContext.getBean(McpConnectionClients.class),
            beanContext.getBean(ExecutorService.class, Qualifiers.byName(TaskExecutors.BLOCKING)));
        McpClientHttpConfiguration configuration = configuration("crafted");
        JsonSchemaValidator validator = beanContext.getBean(JsonSchemaValidator.class);
        IllegalStateException error = assertThrows(IllegalStateException.class, () -> factory.mcpClientSyncSpec(configuration, validator));
        assertTrue(error.getMessage().contains("micronaut-http-client"), error.getMessage());
    }

    @Test
    void late404DoesNotExpireTheReplacementSession() throws Exception {
        McpClientTransport transport = transport("crafted", new CopyOnWriteArrayList<>());
        AtomicInteger expirations = new AtomicInteger();
        transport.setExceptionHandler(ignored -> expirations.incrementAndGet());
        server.on("POST initialize", json(200, INITIALIZE_RESULT, SESSION_ID, "s1"));
        transport.sendMessage(initialize()).block(TIMEOUT);
        CompletableFuture<Void> firstRelease = new CompletableFuture<>();
        CompletableFuture<Void> secondRelease = new CompletableFuture<>();
        server.on("POST tools/list", exchange -> {
            firstRelease.join();
            json(404, "").respond(exchange);
        });
        server.on("POST tools/list", exchange -> {
            secondRelease.join();
            json(404, "").respond(exchange);
        });
        CompletableFuture<Void> first = transport.sendMessage(toolsList("2")).onErrorComplete().toFuture();
        CompletableFuture<Void> second = transport.sendMessage(toolsList("3")).onErrorComplete().toFuture();
        try {
            List<Request> pending = server.await(r -> r.body().contains("tools/list"), 2);
            assertTrue(pending.stream().allMatch(r -> "s1".equals(r.header(SESSION_ID))));
            firstRelease.complete(null);
            CompletableFuture.anyOf(first, second).get(5, TimeUnit.SECONDS);
            server.on("POST initialize", json(200, INITIALIZE_RESULT, SESSION_ID, "s2"));
            transport.sendMessage(initialize()).block(TIMEOUT);
        } finally {
            firstRelease.complete(null);
            secondRelease.complete(null);
        }
        CompletableFuture.allOf(first, second).get(5, TimeUnit.SECONDS);
        assertEquals(1, expirations.get());
    }

    @Test
    void frameBoundaryAcceptsAnExactSizeMessage() {
        McpClientTransport transport = transport("small", new CopyOnWriteArrayList<>());
        String prefix = "{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"result\":{\"text\":\"";
        String suffix = "\"}}";
        String message = prefix + "é" + "x".repeat(1024 - prefix.length() - suffix.length() - 2) + suffix;
        server.on("POST tools/list", events(utf8("data: " + message + "\n\n")));
        transport.sendMessage(toolsList("1")).block(TIMEOUT);
        server.on("POST tools/list", events(utf8("data: " + message + "\r"), utf8("\n\r\n")));
        transport.sendMessage(toolsList("1")).block(TIMEOUT);
    }

    private McpClientTransport transport(String connection, List<McpSchema.JSONRPCMessage> received) {
        McpClientTransport transport = transports.create(configuration(connection));
        transport.connect(message -> message.doOnNext(received::add)).block(TIMEOUT);
        return transport;
    }

    private McpClientHttpConfiguration configuration(String connection) {
        return beanContext.getBean(McpClientHttpConfiguration.class, Qualifiers.byName(connection));
    }

    private static McpSchema.JSONRPCRequest initialize() {
        return new McpSchema.JSONRPCRequest(McpSchema.METHOD_INITIALIZE, "1", null);
    }

    private static McpSchema.JSONRPCNotification initialized() {
        return new McpSchema.JSONRPCNotification(McpSchema.METHOD_NOTIFICATION_INITIALIZED);
    }

    private static McpSchema.JSONRPCRequest toolsList(String id) {
        return new McpSchema.JSONRPCRequest(McpSchema.METHOD_TOOLS_LIST, id, null);
    }

    private static void assertStatus(HttpStatus status, Mono<Void> sent) {
        HttpClientResponseException error = assertThrows(HttpClientResponseException.class, () -> sent.block(TIMEOUT));
        assertEquals(status, error.getStatus());
    }

    private static void assertTooLarge(Mono<Void> sent) {
        IllegalStateException error = assertThrows(IllegalStateException.class, () -> sent.block(TIMEOUT));
        assertTrue(error.getMessage().contains("max-message-size"), error.getMessage());
    }

    private static void pauseLongerThanTheReconnectDelay() {
        LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(1_500));
    }

    @ClientFilter(serviceId = "impatient", patterns = "/**")
    static class ServiceFilter {
        @RequestFilter
        void filter(MutableHttpRequest<?> request) {
            request.header("X-Service", "impatient");
        }
    }
}
