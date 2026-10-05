package io.micronaut.mcp.server.stateless;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.json.JsonMapper;
import io.micronaut.mcp.annotations.Tool;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.Map;

import static io.micronaut.mcp.server.stateless.ExecutionModelSupport.callTool;
import static io.micronaut.mcp.server.stateless.ExecutionModelSupport.text;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Property(name = "micronaut.mcp.server.info.name", value = "mcp-server")
@Property(name = "micronaut.mcp.server.info.version", value = "0.0.1")
@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "spec.name", value = "ToolExecutionErrorsTest")
@MicronautTest
class ToolExecutionErrorsTest {

    @Inject
    @Client("/")
    HttpClient httpClient;

    @Inject
    JsonMapper jsonMapper;

    @Test
    void aFailingToolProducesAToolExecutionError() throws IOException {
        Map<String, Object> result = callTool(httpClient, jsonMapper, "failing");
        assertEquals(Boolean.TRUE, result.get("isError"));
        assertEquals("the backend is down", text(result));
    }

    @Test
    void aFailingPublisherProducesAToolExecutionError() throws IOException {
        Map<String, Object> result = callTool(httpClient, jsonMapper, "failingPublisher");
        assertEquals(Boolean.TRUE, result.get("isError"));
        assertEquals("the backend is down", text(result));
    }

    @Test
    void anMcpErrorIsAProtocolError() throws IOException {
        HttpClientResponseException ex = assertThrows(HttpClientResponseException.class, () -> httpClient.toBlocking().exchange(HttpRequest.POST("/mcp", """
            {"jsonrpc": "2.0", "id": 1, "method": "tools/call", "params": {"name": "protocolError", "arguments": {}}}"""), String.class));
        Map<String, Object> body = jsonMapper.readValue(ex.getResponse().getBody(String.class).orElseThrow(), Argument.mapOf(String.class, Object.class));
        @SuppressWarnings("unchecked")
        Map<String, Object> error = (Map<String, Object>) body.get("error");
        assertEquals(McpSchema.ErrorCodes.INVALID_PARAMS, ((Number) error.get("code")).intValue());
        assertEquals("unknown city", error.get("message"));
    }

    @Requires(property = "spec.name", value = "ToolExecutionErrorsTest")
    @Singleton
    static class Tools {
        @Tool
        String failing() {
            throw new IllegalStateException("the backend is down");
        }

        @Tool
        Mono<String> failingPublisher() {
            return Mono.error(new IllegalStateException("the backend is down"));
        }

        @Tool
        String protocolError() {
            throw McpError.builder(McpSchema.ErrorCodes.INVALID_PARAMS).message("unknown city").build();
        }
    }
}
