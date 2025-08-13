package io.micronaut.mcp.server.stateless.sync;

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
import jakarta.inject.Singleton;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import static io.micronaut.mcp.server.utils.JsonRpcMessages.EXPECTED_TOOLS_CALL;
import static io.micronaut.mcp.server.utils.JsonRpcMessages.EXPECTED_TOOLS_LIST;
import static io.micronaut.mcp.server.utils.JsonRpcMessages.TOOLS_CALL;
import static io.micronaut.mcp.server.utils.JsonRpcMessages.TOOLS_LIST;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@Property(name = "micronaut.mcp.server.type", value = "STATELESS_SYNC")
@Property(name = "spec.name", value = "ToolsTest")
@MicronautTest
class StatelessSyncToolsTest {

    @Test
    void toolsList(@Client("/") HttpClient httpClient) throws JSONException {
        BlockingHttpClient client = httpClient.toBlocking();
        HttpRequest<?> req = HttpRequest.POST("/mcp", TOOLS_LIST);
        String result = assertDoesNotThrow(() -> client.retrieve(req));
        JSONAssert.assertEquals(EXPECTED_TOOLS_LIST, result, true);
    }

    @Test
    void toolsCall(@Client("/") HttpClient httpClient) throws JSONException {
        BlockingHttpClient client = httpClient.toBlocking();
        HttpRequest<?> req = HttpRequest.POST("/mcp", TOOLS_CALL);
        String result = assertDoesNotThrow(() -> client.retrieve(req));
        JSONAssert.assertEquals(EXPECTED_TOOLS_CALL, result, true);
    }

    @Requires(property = "spec.name", value = "ToolsTest")
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
        McpStatelessServerFeatures.SyncToolSpecification getAlertsTools() {
            McpSchema.Tool tool = new McpSchema.Tool("fenEvaluation",
                "Evaluate a chess position using a FEN string.", FEN_EVALUATION_SCHEMA);
            return new McpStatelessServerFeatures.SyncToolSpecification(tool,
                (exchange, arguments) -> {
                    return new McpSchema.CallToolResult("+0.27", false);
                }
            );
        }
    }

}
