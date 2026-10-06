package io.micronaut.mcp.server.stateless;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Property;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Property(name = "micronaut.mcp.server.info.name", value = "mcp-server")
@Property(name = "micronaut.mcp.server.info.version", value = "0.0.1")
@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "micronaut.mcp.server.transport-security.allowed-origins[0]", value = "https://app.example.com")
// Core denies non-local origins on a localhost server; pass them through to test the MCP endpoint rules
@Property(name = "micronaut.server.cors.localhost-pass-through", value = "true")
@MicronautTest
class TransportSecurityTest {

    @Inject
    @Client("/")
    HttpClient httpClient;

    @Test
    void requestsWithoutAnOriginAreAllowed() {
        assertEquals(HttpStatus.OK, status(ping()));
    }

    @Test
    void loopbackAndConfiguredOriginsAreAllowed() {
        assertEquals(HttpStatus.OK, status(ping().header("Origin", "http://localhost:6274")));
        assertEquals(HttpStatus.OK, status(ping().header("Origin", "http://127.0.0.1:3000")));
        assertEquals(HttpStatus.OK, status(ping().header("Origin", "http://[::1]")));
        assertEquals(HttpStatus.OK, status(ping().header("Origin", "https://APP.example.com/")));
    }

    @Test
    void otherOriginsAreForbidden() {
        assertEquals(HttpStatus.FORBIDDEN, status(ping().header("Origin", "http://evil.example")));
        // a DNS rebinding attacker's own name, even if it resolves to a loopback address
        assertEquals(HttpStatus.FORBIDDEN, status(ping().header("Origin", "http://localhost.evil.example")));
        assertEquals(HttpStatus.FORBIDDEN, status(ping().header("Origin", "null")));
    }

    @Test
    void unsupportedProtocolVersionsAreBadRequests() {
        assertEquals(HttpStatus.OK, status(ping().header("MCP-Protocol-Version", "2025-06-18")));
        assertEquals(HttpStatus.OK, status(ping().header("MCP-Protocol-Version", "2025-11-25")));
        assertEquals(HttpStatus.BAD_REQUEST, status(ping().header("MCP-Protocol-Version", "1999-01-01")));
    }

    @Test
    void malformedOriginsAreForbidden() {
        assertEquals(HttpStatus.OK, status(ping().header("Origin", "http://[::1]:6274")));
        assertEquals(HttpStatus.FORBIDDEN, status(ping().header("Origin", "http://[::1")));
        assertEquals(HttpStatus.FORBIDDEN, status(ping().header("Origin", "localhost")));
        assertEquals(HttpStatus.FORBIDDEN, status(ping().header("Origin", "http://localhost@evil.example")));
    }

    @Test
    void loopbackOriginsCanBeDisallowed() {
        try (EmbeddedServer server = ApplicationContext.run(EmbeddedServer.class, Map.of(
                "micronaut.mcp.server.info.name", "mcp-server",
                "micronaut.mcp.server.info.version", "0.0.1",
                "micronaut.mcp.server.transport", "HTTP",
                "micronaut.mcp.server.transport-security.allow-loopback-origins", false,
                "micronaut.mcp.server.transport-security.allowed-origins", List.of("http://localhost:6274"),
                "micronaut.server.cors.localhost-pass-through", true));
             HttpClient client = server.getApplicationContext().createBean(HttpClient.class, server.getURL())) {
            assertEquals(HttpStatus.OK, status(client, ping().header("Origin", "http://localhost:6274")));
            assertEquals(HttpStatus.FORBIDDEN, status(client, ping().header("Origin", "http://localhost:3000")));
            assertEquals(HttpStatus.OK, status(client, ping()));
        }
    }

    @Test
    void validationCanBeDisabled() {
        try (EmbeddedServer server = ApplicationContext.run(EmbeddedServer.class, Map.of(
                "micronaut.mcp.server.info.name", "mcp-server",
                "micronaut.mcp.server.info.version", "0.0.1",
                "micronaut.mcp.server.transport", "HTTP",
                "micronaut.mcp.server.transport-security.enabled", false,
                "micronaut.server.cors.localhost-pass-through", true));
             HttpClient client = server.getApplicationContext().createBean(HttpClient.class, server.getURL())) {
            assertEquals(HttpStatus.OK, client.toBlocking().exchange(ping().header("Origin", "http://evil.example")).getStatus());
        }
    }

    private static MutableHttpRequest<String> ping() {
        return HttpRequest.POST("/mcp", """
            {"jsonrpc": "2.0", "id": 1, "method": "ping"}""");
    }

    private HttpStatus status(HttpRequest<?> request) {
        return status(httpClient, request);
    }

    private static HttpStatus status(HttpClient httpClient, HttpRequest<?> request) {
        try {
            return httpClient.toBlocking().exchange(request).getStatus();
        } catch (HttpClientResponseException e) {
            return e.getStatus();
        }
    }
}
