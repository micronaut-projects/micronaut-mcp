package io.micronaut.mcp.server.context;

import io.micronaut.core.util.LocaleResolver;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.server.util.HttpHostResolver;
import io.modelcontextprotocol.spec.ProtocolVersions;
import org.junit.jupiter.api.Test;

import java.security.Principal;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class HttpRequestMcpTransportContextTest {

    @Test
    void valuesAreReadFromTheRequest() {
        Principal principal = () -> "alice";
        MutableHttpRequest<?> request = HttpRequest.GET("/mcp")
            .header(io.modelcontextprotocol.spec.HttpHeaders.PROTOCOL_VERSION, ProtocolVersions.MCP_2025_06_18)
            .header(io.modelcontextprotocol.spec.HttpHeaders.MCP_SESSION_ID, "session")
            .header(io.modelcontextprotocol.spec.HttpHeaders.LAST_EVENT_ID, "event");
        request.setAttribute(MicronautMcpTransportContextAdapter.PRINCIPAL_KEY, principal);
        AtomicInteger resolutions = new AtomicInteger();
        HttpRequestMcpTransportContext context = new HttpRequestMcpTransportContext(request,
            r -> {
                resolutions.incrementAndGet();
                return "http://localhost";
            },
            new FixedLocaleResolver(resolutions));

        assertEquals(ProtocolVersions.MCP_2025_06_18, context.get(io.modelcontextprotocol.spec.HttpHeaders.PROTOCOL_VERSION));
        assertEquals("session", context.get(io.modelcontextprotocol.spec.HttpHeaders.MCP_SESSION_ID));
        assertEquals("event", context.get(io.modelcontextprotocol.spec.HttpHeaders.LAST_EVENT_ID));
        assertEquals("http://localhost", context.get(HttpHeaders.HOST));
        assertEquals(Locale.FRENCH, context.get(HttpHeaders.ACCEPT_LANGUAGE));
        assertSame(principal, context.get(MicronautMcpTransportContextAdapter.PRINCIPAL_KEY));
        assertNull(context.get("unknown"));

        // the host and the locale are resolved once
        assertEquals("http://localhost", context.host());
        assertEquals(Locale.FRENCH, context.locale());
        assertEquals(2, resolutions.get());

        // the adapter reads the same values through the keys
        MicronautMcpTransportContextAdapter adapter = new MicronautMcpTransportContextAdapter(context);
        assertSame(principal, adapter.principal());
        assertEquals(Locale.FRENCH, adapter.locale());
    }

    @Test
    void theProtocolVersionDefaultsWhenTheHeaderIsMissing() {
        HttpRequestMcpTransportContext context = new HttpRequestMcpTransportContext(HttpRequest.GET("/mcp"),
            r -> null, new FixedLocaleResolver(new AtomicInteger()));
        assertEquals(ProtocolVersions.MCP_2025_03_26, context.protocolVersion());
        assertNull(context.principal());
        assertNull(new MicronautMcpTransportContextAdapter(context).principal());
    }

    private record FixedLocaleResolver(AtomicInteger resolutions) implements LocaleResolver<HttpRequest<?>> {
        @Override
        public Optional<Locale> resolve(HttpRequest<?> request) {
            resolutions.incrementAndGet();
            return Optional.of(Locale.FRENCH);
        }

        @Override
        public Locale resolveOrDefault(HttpRequest<?> request) {
            return resolve(request).orElse(Locale.ENGLISH);
        }
    }
}
