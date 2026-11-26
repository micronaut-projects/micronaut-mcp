package io.micronaut.mcp.server.stateless;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.mcp.annotations.Tool;
import io.micronaut.mcp.annotations.ToolArg;
import io.micronaut.mcp.server.json.MicronautMcpJsonMapper;
import io.micronaut.mcp.server.json.MicronautMcpJsonMapperSupplier;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.TypeRef;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Singleton;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@Property(name = "micronaut.mcp.server.info.name", value = "mcp-server")
@Property(name = "micronaut.mcp.server.info.version", value = "0.0.1")
@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "spec.name", value = "McpControllerReplacedJsonMapperTest")
@MicronautTest
class McpControllerReplacedJsonMapperTest {

    @Test
    void aReplacedMcpJsonMapperGetsTheParametersAsAMap(@Client("/") HttpClient httpClient,
                                                      RecordingMcpJsonMapper mcpJsonMapper) throws JSONException {
        BlockingHttpClient client = httpClient.toBlocking();
        HttpResponse<String> response = client.exchange(HttpRequest.POST("/mcp", """
            {
              "jsonrpc": "2.0",
              "id": 1,
              "method": "tools/call",
              "params": {"name": "echo", "arguments": {"text": "hello"}}
            }"""), String.class);

        assertEquals(HttpStatus.OK, response.getStatus());
        JSONAssert.assertEquals("""
            {
              "jsonrpc": "2.0",
              "id": 1,
              "result": {"content": [{"type": "text", "text": "hello"}], "isError": false}
            }""", response.body(), true);
        assertInstanceOf(Map.class, mcpJsonMapper.callToolRequestSource);
    }

    @Requires(property = "spec.name", value = "McpControllerReplacedJsonMapperTest")
    @Singleton
    static class Tools {
        @Tool
        String echo(@ToolArg(name = "text") String text) {
            return text;
        }
    }

    /**
     * An application {@link McpJsonMapper} that is not a {@link MicronautMcpJsonMapper}. It records what the SDK converts
     * into the tool call request.
     */
    @Requires(property = "spec.name", value = "McpControllerReplacedJsonMapperTest")
    @Replaces(McpJsonMapper.class)
    @Singleton
    static class RecordingMcpJsonMapper implements McpJsonMapper {
        private final McpJsonMapper delegate = new MicronautMcpJsonMapperSupplier().get();
        private volatile Object callToolRequestSource;

        @Override
        public <T> T readValue(String content, Class<T> type) throws IOException {
            return delegate.readValue(content, type);
        }

        @Override
        public <T> T readValue(byte[] content, Class<T> type) throws IOException {
            return delegate.readValue(content, type);
        }

        @Override
        public <T> T readValue(String content, TypeRef<T> type) throws IOException {
            return delegate.readValue(content, type);
        }

        @Override
        public <T> T readValue(byte[] content, TypeRef<T> type) throws IOException {
            return delegate.readValue(content, type);
        }

        @Override
        public <T> T convertValue(Object fromValue, Class<T> type) {
            if (type == McpSchema.CallToolRequest.class) {
                callToolRequestSource = fromValue;
            }
            return delegate.convertValue(fromValue, type);
        }

        @Override
        public <T> T convertValue(Object fromValue, TypeRef<T> type) {
            if (type.getType() == McpSchema.CallToolRequest.class) {
                callToolRequestSource = fromValue;
            }
            return delegate.convertValue(fromValue, type);
        }

        @Override
        public String writeValueAsString(Object value) throws IOException {
            return delegate.writeValueAsString(value);
        }

        @Override
        public byte[] writeValueAsBytes(Object value) throws IOException {
            return delegate.writeValueAsBytes(value);
        }
    }
}
