package io.micronaut.mcp.server.stateless;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.mcp.annotations.Tool;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.modelcontextprotocol.server.McpStatelessServerFeatures;
import io.modelcontextprotocol.server.McpStatelessSyncServer;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Property(name = "micronaut.mcp.server.info.name", value = "mcp-server")
@Property(name = "micronaut.mcp.server.info.version", value = "0.0.1")
@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "micronaut.mcp.server.tools.list-changed", value = "true")
@Property(name = "micronaut.mcp.server.resources.list-changed", value = "true")
@Property(name = "micronaut.mcp.server.resources.subscribe", value = "true")
@Property(name = "spec.name", value = "StatelessDynamicToolsTest")
@MicronautTest
class StatelessDynamicToolsTest {

    @Inject
    @Client("/")
    HttpClient httpClient;

    @Inject
    McpStatelessSyncServer server;

    @Test
    void aStatelessServerDoesNotAdvertiseNotificationsItCannotSend() throws JSONException {
        JSONAssert.assertEquals("""
            {"result": {"capabilities": {"tools": {"listChanged": false}}}}""", call("initialize", """
            {"protocolVersion": "2025-06-18", "capabilities": {}, "clientInfo": {"name": "test", "version": "1"}}"""), JSONCompareMode.LENIENT);
    }

    @Test
    void toolsCanBeAddedAndRemovedAtRuntime() {
        server.addTool(McpStatelessServerFeatures.SyncToolSpecification.builder()
            .tool(McpSchema.Tool.builder().name("added").inputSchema(Map.of("type", "object")).build())
            .callHandler((context, request) -> McpSchema.CallToolResult.builder().addTextContent("added at runtime").build())
            .build());
        assertTrue(call("tools/list", "{}").contains("\"name\":\"added\""));
        assertTrue(call("tools/call", """
            {"name": "added", "arguments": {}}""").contains("added at runtime"));

        server.removeTool("added");
        assertFalse(call("tools/list", "{}").contains("\"name\":\"added\""));
    }

    private String call(String method, String params) {
        return httpClient.toBlocking().retrieve(HttpRequest.POST("/mcp", """
            {"jsonrpc": "2.0", "id": 1, "method": "%s", "params": %s}""".formatted(method, params)));
    }

    @Requires(property = "spec.name", value = "StatelessDynamicToolsTest")
    @Singleton
    static class Tools {
        @Tool
        String fixed() {
            return "fixed";
        }
    }
}
