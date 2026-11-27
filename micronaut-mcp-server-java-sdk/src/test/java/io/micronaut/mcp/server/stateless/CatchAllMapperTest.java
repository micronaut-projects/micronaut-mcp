package io.micronaut.mcp.server.stateless;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.json.JsonMapper;
import io.micronaut.mcp.server.exceptions.McpErrorExceptionMapper;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static io.micronaut.mcp.server.stateless.ExecutionModelSupport.callToolError;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * An application mapper that matches every runtime exception turns tool failures into protocol errors, including an
 * {@link McpError} a tool throws: the registered mappers are asked before an {@link McpError} is sent as is.
 */
@Property(name = "micronaut.mcp.server.info.name", value = "mcp-server")
@Property(name = "micronaut.mcp.server.info.version", value = "0.0.1")
@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "tool-errors.tools", value = "true")
@Property(name = "spec.name", value = "CatchAllMapperTest")
@MicronautTest
class CatchAllMapperTest {

    @Inject
    @Client("/")
    HttpClient httpClient;

    @Inject
    JsonMapper jsonMapper;

    @Test
    void anMcpErrorIsMappedToo() throws IOException {
        Map<String, Object> error = callToolError(httpClient, jsonMapper, "protocolError", HttpStatus.INTERNAL_SERVER_ERROR);
        assertEquals(McpSchema.ErrorCodes.INTERNAL_ERROR, error.get("code"));
        assertEquals("caught: unknown city", error.get("message"));
    }

    @Test
    void anotherFailureIsMappedToAProtocolError() throws IOException {
        Map<String, Object> error = callToolError(httpClient, jsonMapper, "failing", HttpStatus.INTERNAL_SERVER_ERROR);
        assertEquals(McpSchema.ErrorCodes.INTERNAL_ERROR, error.get("code"));
        assertEquals("caught: the backend is down", error.get("message"));
    }

    @Requires(property = "spec.name", value = "CatchAllMapperTest")
    @Singleton
    static class CatchAllMapper implements McpErrorExceptionMapper<RuntimeException> {
        @Override
        public boolean canMap(Class<? extends Throwable> clazz) {
            return RuntimeException.class.isAssignableFrom(clazz);
        }

        @Override
        public McpError map(RuntimeException exception) {
            return McpError.builder(McpSchema.ErrorCodes.INTERNAL_ERROR).message("caught: " + exception.getMessage()).build();
        }
    }
}
