package io.micronaut.mcp.client.langchain4j.multi;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.mcp.client.McpClient;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.io.socket.SocketUtils;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.annotation.ClientFilter;
import io.micronaut.http.annotation.RequestFilter;
import io.micronaut.http.context.ServerRequestContext;
import io.micronaut.mcp.annotations.Tool;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.support.TestPropertyProvider;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Property(name = "micronaut.mcp.server.info.name", value = "http-server")
@Property(name = "micronaut.mcp.server.info.version", value = "1.0.0")
@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "spec.name", value = "LangChain4jMicronautHttpClientTransportTest")
@MicronautTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MicronautHttpClientTransportTest implements TestPropertyProvider {

    @Inject
    @Named("micronaut")
    McpClient client;

    @Override
    public Map<String, String> getProperties() {
        int port = SocketUtils.findAvailableTcpPort();
        return Map.of(
            "micronaut.server.port", String.valueOf(port),
            "micronaut.mcp.client.http.micronaut.url", "http://localhost:" + port + "/mcp",
            "micronaut.mcp.client.http.micronaut.http-client", "MICRONAUT");
    }

    @Test
    void toolsAreListedAndCalledThroughTheMicronautHttpClient() {
        assertTrue(client.listTools().stream().anyMatch(t -> t.name().equals("filtered")));
        String result = client.executeTool(ToolExecutionRequest.builder().name("filtered").arguments("{}").build()).resultText();
        assertEquals("from the client filter", result);
    }

    @Test
    void protocolErrorsReachTheClient() {
        RuntimeException error = assertThrows(RuntimeException.class,
            () -> client.executeTool(ToolExecutionRequest.builder().name("protocolError").arguments("{}").build()));
        assertTrue(String.valueOf(error.getMessage()).contains("bad"), String.valueOf(error));
    }

    @Requires(property = "spec.name", value = "LangChain4jMicronautHttpClientTransportTest")
    @Singleton
    static class Tools {
        @Tool
        String filtered() {
            return ServerRequestContext.currentRequest().map(r -> r.getHeaders().get("X-Filter")).orElse("no request");
        }

        @Tool
        String protocolError() {
            throw McpError.builder(McpSchema.ErrorCodes.INVALID_PARAMS).message("bad").build();
        }
    }

    @Requires(property = "spec.name", value = "LangChain4jMicronautHttpClientTransportTest")
    @ClientFilter("/mcp")
    static class AddHeader {
        @RequestFilter
        void filter(MutableHttpRequest<?> request) {
            request.header("X-Filter", "from the client filter");
        }
    }
}
