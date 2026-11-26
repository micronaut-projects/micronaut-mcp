package io.micronaut.mcp.server.stateless;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.mcp.annotations.Tool;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Without a primitive that declares an {@link io.micronaut.mcp.server.context.McpRequestContext}, the server sends no
 * log messages, so it neither advertises the logging capability nor handles {@code logging/setLevel}.
 */
@Property(name = "micronaut.mcp.server.info.name", value = "mcp-server")
@Property(name = "micronaut.mcp.server.info.version", value = "0.0.1")
@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "spec.name", value = "LoggingCapabilityTest")
@MicronautTest
class LoggingCapabilityTest {

    @Inject
    @Client("/")
    HttpClient httpClient;

    @Test
    void withoutARequestContextLoggingIsNotAdvertisedNorHandled() {
        String initialize = httpClient.toBlocking().retrieve(post("""
            {"jsonrpc": "2.0", "id": 1, "method": "initialize",
             "params": {"protocolVersion": "2025-06-18", "capabilities": {}, "clientInfo": {"name": "test", "version": "1"}}}"""));
        assertTrue(initialize.contains("\"tools\""), initialize);
        assertFalse(initialize.contains("\"logging\""), initialize);
        HttpClientResponseException e = assertThrows(HttpClientResponseException.class, () -> httpClient.toBlocking().retrieve(post("""
            {"jsonrpc": "2.0", "id": 2, "method": "logging/setLevel", "params": {"level": "error"}}""")));
        assertEquals(HttpStatus.BAD_REQUEST, e.getStatus());
        String body = e.getResponse().getBody(String.class).orElseThrow();
        assertTrue(body.contains("\"code\":" + McpSchema.ErrorCodes.METHOD_NOT_FOUND), body);
    }

    private static HttpRequest<String> post(String body) {
        return HttpRequest.POST("/mcp", body)
            .contentType(MediaType.APPLICATION_JSON_TYPE)
            .accept(MediaType.APPLICATION_JSON_TYPE);
    }

    @Requires(property = "spec.name", value = "LoggingCapabilityTest")
    @Singleton
    static class Quiet {
        @Tool
        String quiet() {
            return "quiet";
        }
    }
}
