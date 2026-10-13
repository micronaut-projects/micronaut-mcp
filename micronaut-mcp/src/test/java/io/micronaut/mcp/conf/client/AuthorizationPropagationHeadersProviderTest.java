package io.micronaut.mcp.conf.client;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.context.ServerRequestContext;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AuthorizationPropagationHeadersProviderTest {
    private final AuthorizationPropagationHeadersProvider provider = new AuthorizationPropagationHeadersProvider();

    @Test
    void bearerTokensArePropagated() {
        assertEquals(Map.of("Authorization", "Bearer token"), headers(connection(true, false), "Bearer token"));
    }

    @Test
    void otherSchemesAreNotPropagatedByDefault() {
        assertEquals(Map.of(), headers(connection(true, false), "Basic dXNlcjpwYXNz"));
    }

    @Test
    void otherSchemesArePropagatedWhenConfigured() {
        assertEquals(Map.of("Authorization", "Basic dXNlcjpwYXNz"), headers(connection(true, true), "Basic dXNlcjpwYXNz"));
    }

    @Test
    void connectionsThatDoNotPropagateTheAuthorizationAreNotSupported() {
        McpClientHttpConfiguration connection = connection(false, false);
        assertFalse(provider.supports(connection));
        assertEquals(Map.of(), ServerRequestContext.with(HttpRequest.GET("/").bearerAuth("token"),
            (Supplier<Map<String, String>>) () -> McpClientRequestHeaders.headers(connection, List.of(provider))));
    }

    @Test
    void providersOnlyApplyToTheConnectionsTheySupport() {
        McpClientHeadersProvider weatherOnly = new McpClientHeadersProvider() {
            @Override
            public @NonNull Map<String, String> headers(@NonNull McpClientHttpConfiguration connection) {
                return Map.of("X-Key", "secret");
            }

            @Override
            public boolean supports(@NonNull McpClientHttpConfiguration connection) {
                return connection.getName().equals("weather");
            }
        };
        McpClientHttpConfiguration other = connection(false, false);
        assertFalse(McpClientRequestHeaders.isDynamic(other, List.of(weatherOnly)));
        assertEquals(Map.of(), McpClientRequestHeaders.headers(other, List.of(weatherOnly)));
        assertEquals(Map.of("X-Key", "secret"), McpClientRequestHeaders.headers(McpClientHttpConfiguration.of("weather", URI.create("http://localhost/mcp")), List.of(weatherOnly)));
    }

    private Map<String, String> headers(McpClientHttpConfiguration connection, String authorization) {
        return ServerRequestContext.with(HttpRequest.GET("/").header("Authorization", authorization),
            (Supplier<Map<String, String>>) () -> McpClientRequestHeaders.headers(connection, List.of(provider)));
    }

    private static McpClientHttpConfiguration connection(boolean propagate, boolean anyScheme) {
        McpClientHttpConfiguration base = McpClientHttpConfiguration.of("other", URI.create("http://localhost/mcp"));
        return new McpClientHttpConfiguration() {
            @Override
            public @NonNull URI getUrl() {
                return base.getUrl();
            }

            @Override
            public @NonNull String getName() {
                return base.getName();
            }

            @Override
            public java.time.Duration getTimeout() {
                return base.getTimeout();
            }

            @Override
            public boolean isLogRequests() {
                return false;
            }

            @Override
            public boolean isLogResponses() {
                return false;
            }

            @Override
            public boolean isPropagateAuthorization() {
                return propagate;
            }

            @Override
            public boolean isPropagateAnyAuthorizationScheme() {
                return anyScheme;
            }
        };
    }
}
