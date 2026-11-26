package io.micronaut.mcp.server.context;

import io.micronaut.core.util.LocaleResolver;
import io.micronaut.http.HttpAttributes;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.server.util.HttpHostResolver;
import io.modelcontextprotocol.spec.ProtocolVersions;
import org.jspecify.annotations.Nullable;
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
    void valuesAreReadFromTheRequestThroughTheirKeys() {
        Principal principal = () -> "alice";
        MutableHttpRequest<?> request = requestWithHeaders();
        request.setAttribute(HttpRequestMcpTransportContext.PRINCIPAL_KEY, principal);
        HttpRequestMcpTransportContext context = new HttpRequestMcpTransportContext(request,
            new CountingHostResolver("http://localhost"), new CountingLocaleResolver(Locale.FRENCH));

        assertEquals(ProtocolVersions.MCP_2025_06_18, context.get(io.modelcontextprotocol.spec.HttpHeaders.PROTOCOL_VERSION));
        assertEquals("session", context.get(io.modelcontextprotocol.spec.HttpHeaders.MCP_SESSION_ID));
        assertEquals("event", context.get(io.modelcontextprotocol.spec.HttpHeaders.LAST_EVENT_ID));
        assertEquals("http://localhost", context.get(HttpHeaders.HOST));
        assertEquals(Locale.FRENCH, context.get(HttpHeaders.ACCEPT_LANGUAGE));
        assertSame(principal, context.get(HttpRequestMcpTransportContext.PRINCIPAL_KEY));
        assertNull(context.get("unknown"));
    }

    @Test
    @SuppressWarnings("removal")
    void thePrincipalKeyIsTheMicronautPrincipalAttribute() {
        // the key tools look the principal up with must not change when HttpAttributes.PRINCIPAL is removed
        assertEquals(HttpAttributes.PRINCIPAL.toString(), HttpRequestMcpTransportContext.PRINCIPAL_KEY);
    }

    @Test
    void theHostAndTheLocaleAreNotResolvedUntilAskedFor() {
        CountingHostResolver hostResolver = new CountingHostResolver("http://localhost");
        CountingLocaleResolver localeResolver = new CountingLocaleResolver(Locale.FRENCH);
        HttpRequestMcpTransportContext context = new HttpRequestMcpTransportContext(requestWithHeaders(), hostResolver, localeResolver);

        context.protocolVersion();
        context.sessionId();
        context.lastEventId();
        context.principal();

        assertEquals(0, hostResolver.resolutions.get());
        assertEquals(0, localeResolver.resolutions.get());
    }

    @Test
    void theHostTheLocaleAndThePrincipalAreResolvedOnce() {
        Principal principal = () -> "alice";
        MutableHttpRequest<?> request = requestWithHeaders();
        request.setAttribute(HttpRequestMcpTransportContext.PRINCIPAL_KEY, principal);
        CountingHostResolver hostResolver = new CountingHostResolver("http://localhost");
        CountingLocaleResolver localeResolver = new CountingLocaleResolver(Locale.FRENCH);
        HttpRequestMcpTransportContext context = new HttpRequestMcpTransportContext(request, hostResolver, localeResolver);

        assertEquals("http://localhost", context.host());
        assertEquals("http://localhost", context.get(HttpHeaders.HOST));
        assertEquals(Locale.FRENCH, context.locale());
        assertEquals(Locale.FRENCH, context.get(HttpHeaders.ACCEPT_LANGUAGE));
        assertSame(principal, context.principal());

        assertEquals(1, hostResolver.resolutions.get());
        assertEquals(1, localeResolver.resolutions.get());
        // a later change of the request attribute is not seen
        request.setAttribute(HttpRequestMcpTransportContext.PRINCIPAL_KEY, (Principal) () -> "bob");
        assertSame(principal, context.principal());
    }

    @Test
    void missingValuesAreResolvedOnce() {
        MutableHttpRequest<?> request = HttpRequest.GET("/mcp");
        CountingHostResolver hostResolver = new CountingHostResolver(null);
        CountingLocaleResolver localeResolver = new CountingLocaleResolver(null);
        HttpRequestMcpTransportContext context = new HttpRequestMcpTransportContext(request, hostResolver, localeResolver);

        assertNull(context.host());
        assertNull(context.host());
        assertNull(context.locale());
        assertNull(context.locale());
        assertNull(context.principal());
        request.setAttribute(HttpRequestMcpTransportContext.PRINCIPAL_KEY, (Principal) () -> "bob");
        assertNull(context.principal());

        assertEquals(1, hostResolver.resolutions.get());
        assertEquals(1, localeResolver.resolutions.get());
    }

    @Test
    void headersThatAreMissingAreNullAndTheProtocolVersionHasADefault() {
        HttpRequestMcpTransportContext context = new HttpRequestMcpTransportContext(HttpRequest.GET("/mcp"),
            new CountingHostResolver(null), new CountingLocaleResolver(null));
        assertEquals(ProtocolVersions.MCP_2025_03_26, context.protocolVersion());
        assertNull(context.get(HttpRequestMcpTransportContext.PRINCIPAL_KEY));
        assertNull(context.sessionId());
        assertNull(context.lastEventId());
    }

    private static MutableHttpRequest<?> requestWithHeaders() {
        return HttpRequest.GET("/mcp")
            .header(io.modelcontextprotocol.spec.HttpHeaders.PROTOCOL_VERSION, ProtocolVersions.MCP_2025_06_18)
            .header(io.modelcontextprotocol.spec.HttpHeaders.MCP_SESSION_ID, "session")
            .header(io.modelcontextprotocol.spec.HttpHeaders.LAST_EVENT_ID, "event");
    }

    static final class CountingHostResolver implements HttpHostResolver {
        final AtomicInteger resolutions = new AtomicInteger();
        private final @Nullable String host;

        CountingHostResolver(@Nullable String host) {
            this.host = host;
        }

        @Override
        public String resolve(@Nullable HttpRequest request) {
            resolutions.incrementAndGet();
            return host;
        }
    }

    static final class CountingLocaleResolver implements LocaleResolver<HttpRequest<?>> {
        final AtomicInteger resolutions = new AtomicInteger();
        private final @Nullable Locale locale;

        CountingLocaleResolver(@Nullable Locale locale) {
            this.locale = locale;
        }

        @Override
        public Optional<Locale> resolve(HttpRequest<?> request) {
            resolutions.incrementAndGet();
            return Optional.ofNullable(locale);
        }

        @Override
        public Locale resolveOrDefault(HttpRequest<?> request) {
            return resolve(request).orElse(Locale.ENGLISH);
        }
    }
}
