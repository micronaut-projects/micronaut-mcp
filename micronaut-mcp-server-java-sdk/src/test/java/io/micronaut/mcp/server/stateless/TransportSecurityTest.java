package io.micronaut.mcp.server.stateless;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.exceptions.ConfigurationException;
import io.micronaut.http.HttpMethod;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.mcp.conf.server.TransportSecurityConfiguration;
import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Property(name = "micronaut.mcp.server.info.name", value = "mcp-server")
@Property(name = "micronaut.mcp.server.info.version", value = "0.0.1")
@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "micronaut.mcp.server.transport-security.allowed-origins[0]", value = "https://app.example.com")
@Property(name = "micronaut.mcp.server.transport-security.allowed-origins[1]", value = "https://secure.example.com:443")
@Property(name = "micronaut.mcp.server.transport-security.allowed-origins[2]", value = "http://plain.example.com:80/")
@Property(name = "micronaut.mcp.server.transport-security.allowed-origins[3]", value = "HTTPS://PORT.example.com:8443")
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
        assertEquals(HttpStatus.OK, status(ping().header("Origin", "HTTP://LOCALHOST:1")));
        assertEquals(HttpStatus.OK, status(ping().header("Origin", "https://APP.example.com/")));
    }

    @Test
    void configuredOriginsMatchWithOrWithoutTheirDefaultPort() {
        // browsers leave out the default port of the scheme
        assertEquals(HttpStatus.OK, status(ping().header("Origin", "https://secure.example.com")));
        assertEquals(HttpStatus.OK, status(ping().header("Origin", "http://plain.example.com")));
        assertEquals(HttpStatus.OK, status(ping().header("Origin", "https://app.example.com:443")));
        assertEquals(HttpStatus.OK, status(ping().header("Origin", "https://port.example.com:8443")));
        assertEquals(HttpStatus.FORBIDDEN, status(ping().header("Origin", "https://port.example.com")));
        assertEquals(HttpStatus.FORBIDDEN, status(ping().header("Origin", "http://secure.example.com:443")));
        assertEquals(HttpStatus.FORBIDDEN, status(ping().header("Origin", "https://plain.example.com:80")));
    }

    @Test
    void theRejectionIsAJsonRpcError() {
        HttpClientResponseException e = assertThrows(HttpClientResponseException.class,
            () -> httpClient.toBlocking().exchange(ping().header("Origin", "http://evil.example"), String.class));
        assertEquals(HttpStatus.FORBIDDEN, e.getStatus());
        assertEquals("""
            {"jsonrpc":"2.0","id":null,"error":{"code":-32600,"message":"Origin not allowed: http://evil.example"}}""",
            e.getResponse().getBody(String.class).orElseThrow());
    }

    @Test
    void theFilterAppliesToTheEndpointWithATrailingSlash() {
        assertEquals(HttpStatus.FORBIDDEN, status(ping("/mcp/").header("Origin", "http://evil.example")));
    }

    @Test
    void otherOriginsAreForbidden() {
        assertEquals(HttpStatus.FORBIDDEN, status(ping().header("Origin", "http://evil.example")));
        // a DNS rebinding attacker's own name, even if it resolves to a loopback address
        assertEquals(HttpStatus.FORBIDDEN, status(ping().header("Origin", "http://localhost.evil.example")));
        assertEquals(HttpStatus.FORBIDDEN, status(ping().header("Origin", "null")));
        assertEquals(HttpStatus.FORBIDDEN, status(ping().header("Origin", "")));
    }

    @Test
    void repeatedHeadersAreRejected() {
        // the second value must not pass for the first, whatever their order
        assertEquals(HttpStatus.FORBIDDEN, status(ping().header("Origin", "https://app.example.com").header("Origin", "http://evil.example")));
        assertEquals(HttpStatus.FORBIDDEN, status(ping().header("Origin", "http://localhost:6274").header("Origin", "http://localhost:6274")));
        assertEquals(HttpStatus.BAD_REQUEST, status(ping().header("MCP-Protocol-Version", "2025-11-25").header("MCP-Protocol-Version", "1999-01-01")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "POST", "DELETE", "PUT"})
    void everyMethodOnTheEndpointIsValidated(String method) {
        assertEquals(HttpStatus.FORBIDDEN, status(HttpRequest.create(HttpMethod.parse(method), "/mcp").header("Origin", "http://evil.example")));
        assertEquals(HttpStatus.BAD_REQUEST, status(HttpRequest.create(HttpMethod.parse(method), "/mcp")
            .header("Origin", "http://localhost:6274").header("MCP-Protocol-Version", "1999-01-01")));
    }

    @Test
    void theHeadersAreValidatedBeforeTheBodyIsRead() {
        assertEquals(HttpStatus.FORBIDDEN, status(HttpRequest.POST("/mcp", "{not json").header("Origin", "http://evil.example")));
    }

    @Test
    void onlyTheUsualLoopbackAddressesAreAllowedByDefault() {
        assertEquals(HttpStatus.FORBIDDEN, status(ping().header("Origin", "http://127.0.0.2:6274")));
        assertEquals(HttpStatus.FORBIDDEN, status(ping().header("Origin", "http://[::ffff:127.0.0.1]:6274")));
    }

    @Test
    void unsupportedProtocolVersionsAreBadRequests() {
        assertEquals(HttpStatus.OK, status(ping().header("MCP-Protocol-Version", "2025-03-26")));
        assertEquals(HttpStatus.OK, status(ping().header("MCP-Protocol-Version", "2025-06-18")));
        assertEquals(HttpStatus.OK, status(ping().header("MCP-Protocol-Version", "2025-11-25")));
        assertEquals(HttpStatus.BAD_REQUEST, status(ping().header("MCP-Protocol-Version", "1999-01-01")));
        assertEquals(HttpStatus.BAD_REQUEST, status(ping().header("MCP-Protocol-Version", "")));
    }

    @Test
    void malformedOriginsAreForbidden() {
        assertEquals(HttpStatus.OK, status(ping().header("Origin", "http://[::1]:6274")));
        assertEquals(HttpStatus.FORBIDDEN, status(ping().header("Origin", "http://[::1")));
        assertEquals(HttpStatus.FORBIDDEN, status(ping().header("Origin", "localhost")));
        assertEquals(HttpStatus.FORBIDDEN, status(ping().header("Origin", "http://localhost@evil.example")));
        assertEquals(HttpStatus.FORBIDDEN, status(ping().header("Origin", "http://localhost:6274@evil.example")));
        assertEquals(HttpStatus.FORBIDDEN, status(ping().header("Origin", "http://[::1]:6274@evil.example")));
        assertEquals(HttpStatus.FORBIDDEN, status(ping().header("Origin", "http://localhost:")));
        assertEquals(HttpStatus.FORBIDDEN, status(ping().header("Origin", "http://localhost/evil")));
        assertEquals(HttpStatus.OK, status(ping().header("Origin", "http://localhost:6274/")));
    }

    @Test
    void loopbackOriginsCanBeDisallowed() {
        try (EmbeddedServer server = mcpServer(Map.of(
                "micronaut.mcp.server.transport-security.allow-loopback-origins", false,
                "micronaut.mcp.server.transport-security.allowed-origins", List.of("http://localhost:6274")));
             HttpClient client = server.getApplicationContext().createBean(HttpClient.class, server.getURL())) {
            assertEquals(HttpStatus.OK, status(client, ping().header("Origin", "http://localhost:6274")));
            assertEquals(HttpStatus.FORBIDDEN, status(client, ping().header("Origin", "http://localhost:3000")));
            assertEquals(HttpStatus.OK, status(client, ping()));
        }
    }

    @Test
    void aWildcardAllowsEveryOrigin() {
        try (EmbeddedServer server = mcpServer(Map.of(
                "micronaut.mcp.server.transport-security.allowed-origins", List.of("*")));
             HttpClient client = server.getApplicationContext().createBean(HttpClient.class, server.getURL())) {
            assertEquals(HttpStatus.OK, status(client, ping().header("Origin", "http://evil.example")));
            assertEquals(HttpStatus.OK, status(client, ping().header("Origin", "null")));
            // The protocol version is still validated
            assertEquals(HttpStatus.BAD_REQUEST, status(client, ping().header("MCP-Protocol-Version", "1999-01-01")));
        }
    }

    @Test
    void aWildcardAmongOtherEntriesAllowsEveryOriginEvenWithoutLoopbackOrigins() {
        try (EmbeddedServer server = mcpServer(Map.of(
                "micronaut.mcp.server.transport-security.allow-loopback-origins", false,
                "micronaut.mcp.server.transport-security.allowed-origins", List.of("https://a.example", "*")));
             HttpClient client = server.getApplicationContext().createBean(HttpClient.class, server.getURL())) {
            assertEquals(HttpStatus.OK, status(client, ping().header("Origin", "http://evil.example")));
            assertEquals(HttpStatus.OK, status(client, ping().header("Origin", "http://localhost:6274")));
        }
    }

    @Test
    void aReplacedConfigurationDrivesTheFilter() {
        try (EmbeddedServer server = mcpServer(Map.of("spec.name", "TransportSecurityTestReplacement"));
             HttpClient client = server.getApplicationContext().createBean(HttpClient.class, server.getURL())) {
            assertEquals(HttpStatus.FORBIDDEN, status(client, ping().header("Origin", "http://localhost:6274")));
            assertEquals(HttpStatus.OK, status(client, ping().header("Origin", "https://replaced.example.com")));
        }
    }

    @Test
    void theFilterFollowsAConfiguredEndpoint() {
        try (EmbeddedServer server = mcpServer(Map.of(
                "micronaut.mcp.server.endpoint", "/api/mcp",
                // application-test.properties only lets anonymous requests reach /mcp
                "micronaut.security.intercept-url-map[0].pattern", "/api/mcp/**"));
             HttpClient client = server.getApplicationContext().createBean(HttpClient.class, server.getURL())) {
            assertEquals(HttpStatus.OK, status(client, ping("/api/mcp").header("Origin", "http://localhost:6274")));
            assertEquals(HttpStatus.FORBIDDEN, status(client, ping("/api/mcp").header("Origin", "http://evil.example")));
            assertEquals(HttpStatus.FORBIDDEN, status(client, ping("/api/mcp/").header("Origin", "http://evil.example")));
        }
    }

    @Test
    void aBrowserClientOnAnAllowedOriginPassesTheCorsPreflight() {
        String origin = "https://app.example.com";
        try (EmbeddedServer server = mcpServer(Map.of(
                "micronaut.mcp.server.transport-security.allowed-origins", List.of(origin),
                "micronaut.server.cors.enabled", true,
                "micronaut.server.cors.configurations.web.allowed-origins", List.of(origin)));
             HttpClient client = server.getApplicationContext().createBean(HttpClient.class, server.getURL())) {
            HttpResponse<?> preflight = client.toBlocking().exchange(HttpRequest.OPTIONS("/mcp")
                .header("Origin", origin)
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "content-type,authorization,mcp-protocol-version"));
            assertEquals(HttpStatus.OK, preflight.getStatus());
            assertEquals(origin, preflight.getHeaders().get("Access-Control-Allow-Origin"));
            HttpResponse<?> response = client.toBlocking().exchange(ping().header("Origin", origin)
                .header("MCP-Protocol-Version", "2025-11-25"));
            assertEquals(HttpStatus.OK, response.getStatus());
            assertEquals(origin, response.getHeaders().get("Access-Control-Allow-Origin"));
        }
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "app.example.com | expected * or an origin, scheme://host[:port], without a path, for example 'https://app.example.com'",
        "https://app.example.com/ui | for example 'https://app.example.com'",
        "https:// | expected * or an origin, scheme://host[:port], without a path",
        "https://[] | expected * or an origin, scheme://host[:port], without a path",
        "https://user@app.example.com | expected * or an origin, scheme://host[:port], without a path",
        "https://app example.com | expected * or an origin, scheme://host[:port], without a path",
        "https://[::g1] | expected * or an origin, scheme://host[:port], without a path",
        "null | clients that send Origin: null, such as file:// pages and sandboxed iframes, can only be allowed with *",
        "https://*.example.com | wildcards are not supported in an origin; list each origin, for example https://app.example.com, or use * to allow every origin",
        "http://localhost:* | wildcards are not supported in an origin"})
    void anAllowedOriginThatCanNeverMatchFailsTheStartup(String entry, String hint) {
        Map<String, Object> properties = Map.of("micronaut.mcp.server.transport-security.allowed-origins", List.of(entry));
        Throwable failure = assertThrows(RuntimeException.class, () -> mcpServer(properties).close());
        while (!(failure instanceof ConfigurationException) && failure.getCause() != null) {
            failure = failure.getCause();
        }
        assertInstanceOf(ConfigurationException.class, failure);
        String message = failure.getMessage();
        assertTrue(message.contains("Invalid micronaut.mcp.server.transport-security.allowed-origins entry '" + entry + "': "), message);
        assertTrue(message.contains(hint), message);
        if (!hint.contains("for example")) {
            assertFalse(message.contains("for example '"), message);
        }
    }

    @Test
    void anOriginAllowedByCorsButNotByTheEndpointGetsAReadableRejection() {
        String origin = "https://app.example.com";
        try (EmbeddedServer server = mcpServer(Map.of(
                "micronaut.server.cors.enabled", true,
                "micronaut.server.cors.configurations.web.allowed-origins", List.of(origin)));
             HttpClient client = server.getApplicationContext().createBean(HttpClient.class, server.getURL())) {
            // CORS answers the preflight on its own
            HttpResponse<?> preflight = client.toBlocking().exchange(HttpRequest.OPTIONS("/mcp")
                .header("Origin", origin)
                .header("Access-Control-Request-Method", "POST"));
            assertEquals(HttpStatus.OK, preflight.getStatus());
            HttpClientResponseException e = assertThrows(HttpClientResponseException.class,
                () -> client.toBlocking().exchange(ping().header("Origin", origin), String.class));
            assertEquals(HttpStatus.FORBIDDEN, e.getStatus());
            // so the browser can read the rejection
            assertEquals(origin, e.getResponse().getHeaders().get("Access-Control-Allow-Origin"));
            assertTrue(e.getResponse().getBody(String.class).orElseThrow().contains("Origin not allowed: " + origin));
        }
    }

    @Test
    void rejectionsAreLoggedAtDebugLevel() {
        Logger logger = (Logger) LoggerFactory.getLogger(McpTransportSecurityFilter.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        Level level = logger.getLevel();
        logger.setLevel(Level.DEBUG);
        logger.addAppender(appender);
        try {
            // nothing below DEBUG level is logged
            logger.setLevel(Level.INFO);
            status(ping().header("Origin", "http://evil.example"));
            assertEquals(List.of(), appender.list);
            logger.setLevel(Level.DEBUG);
            status(ping().header("Origin", "http://evil.example\tforged"));
            status(ping().header("Origin", "http://localhost:1").header("Origin", "http://localhost:2"));
            status(ping().header("MCP-Protocol-Version", "1999-01-01"));
            status(ping().header("MCP-Protocol-Version", "2025-11-25").header("MCP-Protocol-Version", "2025-11-25"));
        } finally {
            logger.detachAppender(appender);
            logger.setLevel(level);
        }
        assertEquals(List.of(
            "Rejected request to /mcp: Origin 'http://evil.example?forged' is not listed in "
                + "micronaut.mcp.server.transport-security.allowed-origins and is not a loopback origin",
            "Rejected request to /mcp: 2 Origin headers",
            "Rejected request to /mcp: MCP-Protocol-Version '1999-01-01' is not supported (supported: "
                + "[2025-03-26, 2025-06-18, 2025-11-25])",
            "Rejected request to /mcp: 2 MCP-Protocol-Version headers"),
            appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList());
    }

    @Test
    void validationCanBeDisabled() {
        try (EmbeddedServer server = mcpServer(Map.of(
                "micronaut.mcp.server.transport-security.enabled", false));
             HttpClient client = server.getApplicationContext().createBean(HttpClient.class, server.getURL())) {
            assertEquals(HttpStatus.OK, status(client, ping().header("Origin", "http://evil.example")));
            // both checks are off
            assertEquals(HttpStatus.OK, status(client, ping().header("MCP-Protocol-Version", "1999-01-01")));
        }
    }

    @Test
    void invalidAllowedOriginsDoNotFailTheStartupWhenValidationIsDisabled() {
        try (EmbeddedServer server = mcpServer(Map.of(
                "micronaut.mcp.server.transport-security.enabled", false,
                "micronaut.mcp.server.transport-security.allowed-origins", List.of("app.example.com")))) {
            assertTrue(server.isRunning());
        }
    }

    @Test
    void anHttpServerWithoutTheHttpTransportHasNoFilter() {
        for (Map<String, Object> properties : List.of(
                Map.<String, Object>of("micronaut.server.port", -1, "micronaut.server.cors.localhost-pass-through", true),
                Map.<String, Object>of("micronaut.server.port", -1, "micronaut.server.cors.localhost-pass-through", true,
                    "micronaut.mcp.server.transport", "STDIO"))) {
            try (EmbeddedServer server = ApplicationContext.run(EmbeddedServer.class, properties);
                 HttpClient client = server.getApplicationContext().createBean(HttpClient.class, server.getURL())) {
                assertFalse(server.getApplicationContext().containsBean(McpTransportSecurityFilter.class));
                // Not found rather than a failure to create the filter
                assertEquals(HttpStatus.NOT_FOUND, status(client, ping().header("Origin", "http://evil.example")));
            }
        }
    }

    /**
     * Starts a server with the HTTP transport, the given transport security properties, and the CORS pass-through that
     * lets non-local origins reach the MCP endpoint rules.
     */
    private static EmbeddedServer mcpServer(Map<String, Object> transportSecurityProperties) {
        Map<String, Object> properties = new HashMap<>(transportSecurityProperties);
        properties.put("micronaut.mcp.server.info.name", "mcp-server");
        properties.put("micronaut.mcp.server.info.version", "0.0.1");
        properties.put("micronaut.mcp.server.transport", "HTTP");
        properties.put("micronaut.server.cors.localhost-pass-through", true);
        return ApplicationContext.run(EmbeddedServer.class, properties);
    }

    @Requires(property = "spec.name", value = "TransportSecurityTestReplacement")
    @Replaces(TransportSecurityConfiguration.class)
    @Singleton
    static class ReplacedTransportSecurityConfiguration implements TransportSecurityConfiguration {
        @Override
        public List<String> getAllowedOrigins() {
            return List.of("https://replaced.example.com");
        }

        @Override
        public boolean isAllowLoopbackOrigins() {
            return false;
        }
    }

    private static MutableHttpRequest<String> ping() {
        return ping("/mcp");
    }

    private static MutableHttpRequest<String> ping(String path) {
        return HttpRequest.POST(path, """
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
