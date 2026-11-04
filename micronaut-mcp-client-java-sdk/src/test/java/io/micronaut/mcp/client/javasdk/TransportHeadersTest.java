package io.micronaut.mcp.client.javasdk;

import io.micronaut.core.propagation.PropagatedContext;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.context.ServerHttpRequestContext;
import io.micronaut.http.context.ServerRequestContext;
import io.micronaut.mcp.conf.client.McpClientHeadersProvider;
import io.micronaut.mcp.conf.client.McpClientHttpConfiguration;
import io.modelcontextprotocol.common.McpTransportContext;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;
import reactor.util.context.Context;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransportHeadersTest {
    private static final McpClientHttpConfiguration CONNECTION = new McpClientHttpConfiguration() {
        @Override
        public @NonNull URI getUrl() {
            return URI.create("http://localhost/mcp");
        }

        @Override
        public Duration getTimeout() {
            return null;
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
        public @NonNull Map<String, String> getHeaders() {
            return Map.of("X-Static", "static");
        }

        @Override
        public @NonNull String getName() {
            return "headers";
        }
    };

    @Test
    void theHeadersOfAConnectionWithoutProvidersAreStatic() {
        assertEquals(Map.of("X-Static", "static"), TransportHeaders.headers(CONNECTION, List.of(), McpTransportContext.EMPTY, Context.empty()));
    }

    @Test
    void theHeadersProvidersRunWithoutAPropagatedContext() {
        McpClientHeadersProvider provider = connection -> Map.of("X-Dynamic", connection.getName());
        assertEquals(Map.of("X-Static", "static", "X-Dynamic", "headers"),
            TransportHeaders.headers(CONNECTION, List.of(provider), McpTransportContext.EMPTY, Context.empty()));
    }

    @Test
    void theHeadersProvidersDoNotSeeTheRequestOfTheCurrentThreadWithoutAPropagatedContext() {
        McpClientHeadersProvider provider = connection -> Map.of("X-Request", String.valueOf(ServerRequestContext.currentRequest().isPresent()));
        try (var _ = ServerHttpRequestContext.withRequest(PropagatedContext.empty(), HttpRequest.GET("/other-user")).propagate()) {
            assertTrue(ServerRequestContext.currentRequest().isPresent());
            assertEquals(Map.of("X-Static", "static", "X-Request", "false"),
                TransportHeaders.headers(CONNECTION, List.of(provider), McpTransportContext.EMPTY, Context.empty()));
            // the context of the thread is back after the headers are computed
            assertTrue(ServerRequestContext.currentRequest().isPresent());
        }
    }

    @Test
    void theHeadersOfTheTransportContextOverrideTheStaticHeaders() {
        McpTransportContext context = McpClientTransportHeaders.transportContext(Map.of("X-Static", "supplied"));
        assertEquals(Map.of("X-Static", "supplied"), TransportHeaders.headers(CONNECTION, List.of(), context, Context.empty()));
    }
}
