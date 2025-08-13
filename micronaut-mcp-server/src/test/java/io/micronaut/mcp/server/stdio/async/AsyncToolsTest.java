package io.micronaut.mcp.server.stdio.async;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Prototype;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.context.annotation.Requires;
import io.micronaut.mcp.server.utils.Stdio;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpServerTransportProvider;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Property(name = "spec.name", value = "AsyncToolsTest")
@Property(name = "micronaut.mcp.server.type", value = "ASYNC")
@MicronautTest(startApplication = false)
class AsyncToolsTest {

    @Inject
    SyncInitializeTestFactory factory;

    @Test
    void asyncTools() throws JSONException, IOException, InterruptedException {
        String initialize = """
             {"jsonrpc":"2.0","id":0,"method":"initialize","params":{"protocolVersion":"2025-06-18","capabilities":{"sampling":{},"elicitation":{},"roots":{"listChanged":true}},"clientInfo":{"name":"mcp-inspector","version":"0.16.3"}}}""";
        factory.stdio.sendRequest(initialize);
        Thread.sleep(TimeUnit.SECONDS.toMillis(1));
        factory.stdio.sendRequest("""
            {"jsonrpc": "2.0", "method": "notifications/initialized"}""");
        factory.stdio.sendRequest("""
            {"jsonrpc":"2.0","id":3,"method":"tools/list","params":{"_meta":{"progressToken":3}}}""");
        Thread.sleep(TimeUnit.SECONDS.toMillis(1));
        factory.stdio.sendRequest("""
            {"jsonrpc":"2.0","id":4,"method":"tools/call","params":{"_meta":{"progressToken":4},"name":"fenEvaluation","arguments":{"fen":"\\n        String expected = \\"\\"\\"\\n    {\\"jsonrpc\\":\\"2.0\\",\\"id\\":2,\\"result\\":{\\"tools\\":[{\\"name\\":\\"fenEvaluation\\",\\"description\\":\\"Evaluate a chess position using a FEN string.\\",\\"inputSchema\\":{\\"type\\":\\"object\\",\\"properties\\":{\\"fen\\":{\\"type\\":\\"string\\"}}}}]}}\\n    \\"\\"\\";"}}}""");
        Thread.sleep(TimeUnit.SECONDS.toMillis(1));
        List<String> responses = factory.stdio.readResponses();
        assertEquals(3, responses.size());
        String responseJson = responses.get(1);
        assertNotNull(responseJson);
        String expected = """
    {"jsonrpc":"2.0","id":3,"result":{"tools":[{"name":"fenEvaluation","description":"Evaluate a chess position using a FEN string.","inputSchema":{"type":"object","properties":{"fen":{"type":"string"}}}}]}}
    """;
        JSONAssert.assertEquals(expected, responseJson, true);
        String toolCallResponse = responses.get(2);
        String toolCallExpected = """
{
  "jsonrpc": "2.0",
  "id": 4,
  "result": {
    "content": [
      {
        "type": "text",
        "text": "+0.27"
      }
    ],
    "isError": false
  }
}""";
        JSONAssert.assertEquals(toolCallExpected, toolCallResponse, true);
    }

    @Requires(property = "spec.name", value = "AsyncToolsTest")
    @Factory
    static class SyncInitializeTestFactory implements AutoCloseable{
        public final Stdio stdio = new Stdio();

        @Prototype
        @Replaces(McpServerTransportProvider.class)
        McpServerTransportProvider stdioServerTransportProviderReplacement(ObjectMapper objectMapper) {
            return new StdioServerTransportProvider(objectMapper, stdio.serverStdin, stdio.serverStdout);
        }

        @PreDestroy
        @Override
        public void close() {
            stdio.close();
        }
    }

    @Requires(property = "spec.name", value = "AsyncToolsTest")
    @Factory
    static class FenEvaluationTool {
        public static final String FEN_EVALUATION_SCHEMA = """
            {
              "type" : "object",
              "id" : "urn:jsonschema:Operation",
              "properties" : {
                "fen" : {
                  "type" : "string"
                }
              }
            }
            """;

        @Singleton
        McpServerFeatures.AsyncToolSpecification getAlertsTools() {
            McpSchema.Tool tool = new McpSchema.Tool("fenEvaluation",
                "Evaluate a chess position using a FEN string.", FEN_EVALUATION_SCHEMA);
            return new McpServerFeatures.AsyncToolSpecification(tool,
                (exchange, arguments) -> Mono.just(new McpSchema.CallToolResult("+0.27", false))
            );
        }
    }

}
