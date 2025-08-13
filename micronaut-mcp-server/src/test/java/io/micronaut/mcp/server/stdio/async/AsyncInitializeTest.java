package io.micronaut.mcp.server.stdio.async;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Prototype;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.context.annotation.Requires;
import io.micronaut.mcp.server.utils.Stdio;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpServerTransportProvider;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @see <a href="https://modelcontextprotocol.io/specification/2025-06-18/basic/lifecycle#initialization">Initialization</a>
 */
@Property(name = "spec.name", value = "AsyncInitializeTest")
@Property(name = "micronaut.mcp.server.type", value = "ASYNC")
@Property(name = "micronaut.mcp.server.info.name", value = "mcp-server")
@Property(name = "micronaut.mcp.server.info.version", value = "0.0.1")
@MicronautTest(startApplication = false)
class AsyncInitializeTest {

    @Inject
    AsyncInitializeTestFactory factory;

    @Test
    void asyncInitialize() throws JSONException, IOException {
        String json = """
             {"jsonrpc":"2.0","id":0,"method":"initialize","params":{"protocolVersion":"2025-06-18","capabilities":{"sampling":{},"elicitation":{},"roots":{"listChanged":true}},"clientInfo":{"name":"mcp-inspector","version":"0.16.3"}}}""";
        factory.stdio.sendRequest(json);
        List<String> responses = factory.stdio.readResponses();
        String responseJson = responses.get(0);
        assertNotNull(responseJson);
        JSONAssert.assertEquals("""
            {
              "jsonrpc":"2.0",
              "id":0,
              "result": {
                "protocolVersion":"2024-11-05",
                 "capabilities": {},
                 "serverInfo": {
                   "name": "mcp-server",
                   "version": "0.0.1"
                 }
               }
            }""", responseJson, true);
    }

    @Requires(property = "spec.name", value = "AsyncInitializeTest")
    @Factory
    static class AsyncInitializeTestFactory implements AutoCloseable {
        public final Stdio stdio = new Stdio();

        @Prototype
        @Replaces(McpServerTransportProvider.class)
        McpServerTransportProvider stdioServerTransportProviderReplacement(ObjectMapper objectMapper) {
            return new StdioServerTransportProvider(objectMapper, stdio.serverStdin, stdio.serverStdout);
        }

        @PreDestroy
        @Override
        public void close() throws IOException {
            stdio.close();
        }
    }

}
