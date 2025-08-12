package io.micronaut.mcp.server.sdk.stateless.async;

import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.modelcontextprotocol.server.McpStatelessServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@Property(name = "micronaut.mcp.server.info.name", value="world-chess-championship-2024-pgn")
@Property(name = "micronaut.mcp.server.info.version", value="0.0.1")
@Property(name = "micronaut.mcp.server.type", value = "STATELESS_ASYNC")
@Property(name = "spec.name", value = "StatelessAsyncToolsTest")
@MicronautTest
class StatelessAsyncToolsTest {

    @Test
    void toolsList(@Client("/") HttpClient httpClient) throws JSONException {
        BlockingHttpClient client = httpClient.toBlocking();
        String input = """
            {"jsonrpc":"2.0","id":3,"method":"tools/list","params":{"_meta":{"progressToken":3}}}
            """;
        HttpRequest<?> req = HttpRequest.POST("/mcp", input);
        String result = assertDoesNotThrow(() -> client.retrieve(req));
        String expected = """
    {"jsonrpc":"2.0","id":3,"result":{"tools":[{"name":"fenEvaluation","description":"Evaluate a chess position using a FEN string.","inputSchema":{"type":"object","properties":{"fen":{"type":"string"}}}}]}}
    """;
        JSONAssert.assertEquals(expected, result, true);
    }

    @Test
    void toolsCall(@Client("/") HttpClient httpClient) throws JSONException {
        BlockingHttpClient client = httpClient.toBlocking();
        String input = """
            {"jsonrpc":"2.0","id":4,"method":"tools/call","params":{"_meta":{"progressToken":4},"name":"fenEvaluation","arguments":{"fen":"\\n        String expected = \\"\\"\\"\\n    {\\"jsonrpc\\":\\"2.0\\",\\"id\\":2,\\"result\\":{\\"tools\\":[{\\"name\\":\\"fenEvaluation\\",\\"description\\":\\"Evaluate a chess position using a FEN string.\\",\\"inputSchema\\":{\\"type\\":\\"object\\",\\"properties\\":{\\"fen\\":{\\"type\\":\\"string\\"}}}}]}}\\n    \\"\\"\\";"}}}
            """;
        HttpRequest<?> req = HttpRequest.POST("/mcp", input);
        String result = assertDoesNotThrow(() -> client.retrieve(req));
        String expected = """
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
        JSONAssert.assertEquals(expected, result, true);
    }



    @Requires(property = "spec.name", value = "StatelessAsyncToolsTest")
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
            """;;

        @Named("fenEvaluation")
        @Singleton
        McpStatelessServerFeatures.AsyncToolSpecification getAlertsTools() {
            McpSchema.Tool tool = new McpSchema.Tool("fenEvaluation",
                "Evaluate a chess position using a FEN string.", FEN_EVALUATION_SCHEMA);
            return new McpStatelessServerFeatures.AsyncToolSpecification(tool,
                (exchange, arguments) -> {
                    //TODO invoke stockfish
                    String evaluation = "+0.27";
                    return Mono.just(result(evaluation));
                }
            );
        }

        private McpSchema.CallToolResult result(String text) {
            List<McpSchema.Content> contents = new ArrayList<>();
            contents.add(new McpSchema.TextContent(text));
            return new McpSchema.CallToolResult(contents, false);
        }
    }

}
