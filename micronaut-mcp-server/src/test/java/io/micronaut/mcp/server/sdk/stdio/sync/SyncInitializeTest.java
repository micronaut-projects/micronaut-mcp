package io.micronaut.mcp.server.sdk.stdio.sync;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Prototype;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpServerTransportProvider;
import jakarta.inject.Inject;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @see <a href="https://modelcontextprotocol.io/specification/2025-06-18/basic/lifecycle#initialization">Initialization</a>
 */
@Property(name = "spec.name", value = "SyncInitializeTest")
@Property(name = "micronaut.mcp.server.type", value = "SYNC")
@Property(name = "micronaut.mcp.server.info.name", value = "mcp-server")
@Property(name = "micronaut.mcp.server.info.version", value = "0.0.1")
@MicronautTest
class SyncInitializeTest {

    @Inject
    StdioServerTransportProviderReplacementFactory factory;

    @Test
    void initializeSerialization() throws JSONException {
        String responseJson = factory.getResponse();
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

    @Requires(property = "spec.name", value = "SyncInitializeTest")
    @Factory
    static class StdioServerTransportProviderReplacementFactory {
        String json = """
             {"jsonrpc":"2.0","id":0,"method":"initialize","params":{"protocolVersion":"2025-06-18","capabilities":{"sampling":{},"elicitation":{},"roots":{"listChanged":true}},"clientInfo":{"name":"mcp-inspector","version":"0.16.3"}}}""";

        InputStream inputStream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        @Prototype
        @Replaces(McpServerTransportProvider.class)
        McpServerTransportProvider stdioServerTransportProviderReplacement(ObjectMapper objectMapper) {
            return new StdioServerTransportProvider(objectMapper, inputStream, outputStream);
        }

        public String getResponse() {
            return outputStream.toString(StandardCharsets.UTF_8);
        }
    }

}
