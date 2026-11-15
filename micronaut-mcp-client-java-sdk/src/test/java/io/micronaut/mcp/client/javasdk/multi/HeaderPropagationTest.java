package io.micronaut.mcp.client.javasdk.multi;

import io.micronaut.context.BeanProvider;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.io.socket.SocketUtils;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.context.ServerRequestContext;
import io.micronaut.mcp.annotations.Tool;
import io.micronaut.mcp.conf.client.McpClientHeadersProvider;
import io.micronaut.mcp.conf.client.McpClientHttpConfiguration;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.support.TestPropertyProvider;
import io.micronaut.mcp.client.javasdk.McpClientTransportHeaders;
import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import reactor.core.publisher.Mono;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Property(name = "micronaut.mcp.server.info.name", value = "http-server")
@Property(name = "micronaut.mcp.server.info.version", value = "1.0.0")
@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "moon.enabled", value = "false")
@Property(name = "spec.name", value = "HeaderPropagationTest")
@MicronautTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HeaderPropagationTest implements TestPropertyProvider {

    @Inject
    @Client("/")
    HttpClient httpClient;

    @Override
    public Map<String, String> getProperties() {
        int port = SocketUtils.findAvailableTcpPort();
        return Map.ofEntries(
            Map.entry("micronaut.server.port", String.valueOf(port)),
            Map.entry("micronaut.mcp.client.http.self.url", "http://localhost:" + port + "/mcp"),
            Map.entry("micronaut.mcp.client.http.self.propagate-authorization", "true"),
            Map.entry("micronaut.mcp.client.http.self.headers.X-Static", "static"),
            Map.entry("micronaut.mcp.client.http.selfasync.url", "http://localhost:" + port + "/mcp"),
            Map.entry("micronaut.mcp.client.http.selfasync.propagate-authorization", "true"),
            Map.entry("micronaut.mcp.client.http.selfasync.headers.X-Static", "static-async"),
            Map.entry("micronaut.mcp.client.http.selfmicronaut.url", "http://localhost:" + port + "/mcp"),
            Map.entry("micronaut.mcp.client.http.selfmicronaut.http-client", "MICRONAUT"),
            Map.entry("micronaut.mcp.client.http.selfmicronaut.propagate-authorization", "true"),
            Map.entry("micronaut.mcp.client.http.selfmicronaut.headers.X-Static", "static-micronaut"));
    }

    @Test
    void theAuthorizationOfTheRequestAndProvidedHeadersReachTheMcpServer() {
        String seen = httpClient.toBlocking().retrieve(HttpRequest.GET("/proxy").bearerAuth("user-token"));
        assertEquals("Bearer user-token | acme | static", seen);
    }

    @Test
    void theAsynchronousClientPropagatesTheAuthorizationOfTheReactiveRequest() {
        String seen = httpClient.toBlocking().retrieve(HttpRequest.GET("/proxy-async").bearerAuth("async-token"));
        assertEquals("Bearer async-token | acme | static-async", seen);
    }

    @Test
    void theAsynchronousClientSendsTheHeadersOfTheReactorContext() {
        String seen = httpClient.toBlocking().retrieve(HttpRequest.GET("/proxy-async-explicit"));
        assertEquals("Bearer explicit | null | static-async", seen);
    }

    @Test
    void theMicronautHttpClientPropagatesTheAuthorizationOfTheRequest() {
        String seen = httpClient.toBlocking().retrieve(HttpRequest.GET("/proxy-micronaut").bearerAuth("micronaut-token"));
        assertEquals("Bearer micronaut-token | acme | static-micronaut", seen);
    }

    @Test
    void basicCredentialsAreNotPropagatedByDefault() {
        String seen = httpClient.toBlocking().retrieve(HttpRequest.GET("/proxy").basicAuth("user", "secret"));
        assertEquals("null | acme | static", seen);
    }

    @Requires(property = "spec.name", value = "HeaderPropagationTest")
    @Controller
    static class ProxyController {
        private final McpSyncClient client;
        private final McpAsyncClient asyncClient;

        ProxyController(@Named("self") McpSyncClient client, @Named("selfasync") McpAsyncClient asyncClient) {
            this.client = client;
            this.asyncClient = asyncClient;
        }

        @Get("/proxy")
        @ExecuteOn(TaskExecutors.BLOCKING)
        String proxy() {
            if (!client.isInitialized()) {
                client.initialize();
            }
            McpSchema.CallToolResult result = client.callTool(new McpSchema.CallToolRequest("headers", Map.of()));
            return ((McpSchema.TextContent) result.content().getFirst()).text();
        }

        @Get("/proxy-async")
        Mono<String> proxyAsync() {
            return asyncClient.callTool(new McpSchema.CallToolRequest("headers", Map.of()))
                .map(result -> ((McpSchema.TextContent) result.content().getFirst()).text());
        }

        @Get("/proxy-async-explicit")
        Mono<String> proxyAsyncExplicit() {
            return asyncClient.callTool(new McpSchema.CallToolRequest("headers", Map.of()))
                .map(result -> ((McpSchema.TextContent) result.content().getFirst()).text())
                .contextWrite(context -> McpClientTransportHeaders.withHeaders(context, Map.of("Authorization", "Bearer explicit")));
        }
    }

    @Requires(property = "spec.name", value = "HeaderPropagationTest")
    @Controller
    static class MicronautProxyController {
        private final BeanProvider<McpSyncClient> client;

        MicronautProxyController(@Named("selfmicronaut") BeanProvider<McpSyncClient> client) {
            this.client = client;
        }

        @Get("/proxy-micronaut")
        @ExecuteOn(TaskExecutors.BLOCKING)
        String proxy() {
            McpSchema.CallToolResult result = client.get().callTool(new McpSchema.CallToolRequest("headers", Map.of()));
            return ((McpSchema.TextContent) result.content().getFirst()).text();
        }
    }

    @Requires(property = "spec.name", value = "HeaderPropagationTest")
    @Singleton
    static class Tools {
        @Tool
        String headers() {
            return ServerRequestContext.currentRequest()
                .map(r -> r.getHeaders().get("Authorization") + " | " + r.getHeaders().get("X-Tenant") + " | " + r.getHeaders().get("X-Static"))
                .orElse("no request");
        }
    }

    @Requires(property = "spec.name", value = "HeaderPropagationTest")
    @Singleton
    static class TenantHeaders implements McpClientHeadersProvider {
        @Override
        public Map<String, String> headers(McpClientHttpConfiguration connection) {
            return Map.of("X-Tenant", "acme");
        }
    }
}
