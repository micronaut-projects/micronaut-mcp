package io.micronaut.mcp.http.server;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.RequestBean;
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@Property(name = "spec.name", value = "McpRequestTypedRequestArgumentBinderTest")
@MicronautTest
class McpRequestTypedRequestArgumentBinderTest {

    @Test
    void defaultProtocolVersion(@Client("/") HttpClient httpClient) {
        BlockingHttpClient client = httpClient.toBlocking();
        Map<String, String> json = assertDoesNotThrow(() -> client.retrieve(HttpRequest.GET("/mcp/request"), Argument.mapOf(String.class, String.class)));
        assertEquals("2025-03-26", json.get("protocol-version"));
        assertNull(json.get("session-id"));
        assertNull(json.get("last-event-id"));

    }

    @Test
    void protocolVersionViaHeader(@Client("/") HttpClient httpClient) {
        BlockingHttpClient client = httpClient.toBlocking();
        Map<String, String> json = assertDoesNotThrow(() -> client.retrieve(HttpRequest.GET("/mcp/request")
            .header("MCP-Protocol-Version", "2025-06-18"), Argument.mapOf(String.class, String.class)));
        assertEquals("2025-06-18", json.get("protocol-version"));
    }

    @Test
    void sessionIdViaHeader(@Client("/") HttpClient httpClient) {
        BlockingHttpClient client = httpClient.toBlocking();
        Map<String, String> json = assertDoesNotThrow(() -> client.retrieve(HttpRequest.GET("/mcp/request")
                .header("MCP-Protocol-Version", "2025-06-18")
                .header("Mcp-Session-Id", "125694"), Argument.mapOf(String.class, String.class)));
        assertEquals("125694", json.get("session-id"));
    }

    @Test
    void lastEventIdViaHeader(@Client("/") HttpClient httpClient) {
        BlockingHttpClient client = httpClient.toBlocking();
        Map<String, String> json = assertDoesNotThrow(() -> client.retrieve(HttpRequest.GET("/mcp/request")
                .header("Last-Event-ID", "123"), Argument.mapOf(String.class, String.class)));
        assertEquals("123", json.get("last-event-id"));
    }

    @Controller("/mcp/request")
    @Requires(property = "spec.name", value = "McpRequestTypedRequestArgumentBinderTest")
    static class McpRequestEchoController {
        @Get
        Map<String, String> echo(@RequestBean McpHttpRequest mcpHttpRequest) {
            Map<String, String> result = new HashMap<>();
            if (StringUtils.isNotBlank(mcpHttpRequest.protocolVersion())) {
                result.put("protocol-version", mcpHttpRequest.protocolVersion());
            }
            if (StringUtils.isNotBlank(mcpHttpRequest.sessionId())) {
                result.put("session-id", mcpHttpRequest.sessionId());
            }
            if (StringUtils.isNotBlank(mcpHttpRequest.lastEventId())) {
                result.put("last-event-id", mcpHttpRequest.lastEventId());
            }
            return result;
        }
    }
}
