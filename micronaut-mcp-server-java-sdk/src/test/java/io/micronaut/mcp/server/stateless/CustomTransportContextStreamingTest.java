package io.micronaut.mcp.server.stateless;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.mcp.annotations.Tool;
import io.micronaut.mcp.server.context.McpRequestContext;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.server.McpTransportContextExtractor;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A replaced {@link McpTransportContextExtractor} gives a transport context that does not carry the HTTP request; the
 * notifications of the request are still streamed.
 */
@Property(name = "micronaut.mcp.server.info.name", value = "mcp-server")
@Property(name = "micronaut.mcp.server.info.version", value = "0.0.1")
@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "spec.name", value = "CustomTransportContextStreamingTest")
@MicronautTest
class CustomTransportContextStreamingTest {

    @Inject
    @Client("/")
    HttpClient httpClient;

    @Test
    void notificationsAreStreamedWithACustomTransportContext() {
        HttpResponse<String> response = httpClient.toBlocking().exchange(HttpRequest.POST("/mcp", """
            {"jsonrpc": "2.0", "id": 1, "method": "tools/call", "params": {"name": "custom", "arguments": {}}}""")
            .contentType(MediaType.APPLICATION_JSON_TYPE)
            .header("Accept", "application/json, text/event-stream"), String.class);
        assertTrue(response.getContentType().map(MediaType.TEXT_EVENT_STREAM_TYPE::matches).orElse(false), response.body());
        assertTrue(response.body().contains("notifications/message") && response.body().contains("tenant acme"), response.body());
        assertTrue(response.body().contains("\"text\":\"custom\""), response.body());
    }

    @Requires(property = "spec.name", value = "CustomTransportContextStreamingTest")
    @Replaces(McpTransportContextExtractor.class)
    @Singleton
    static class CustomExtractor implements McpTransportContextExtractor<HttpRequest<?>> {
        @Override
        public McpTransportContext extract(HttpRequest<?> request) {
            return McpTransportContext.create(Map.of("tenant", "acme"));
        }
    }

    @Requires(property = "spec.name", value = "CustomTransportContextStreamingTest")
    @Singleton
    static class Tools {
        @Tool
        String custom(McpRequestContext context) {
            context.log(McpSchema.LoggingLevel.INFO, "tenant " + context.transportContext().get("tenant"));
            return "custom";
        }
    }
}
