package io.micronaut.mcp.server.stateless.async;

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

import static io.micronaut.mcp.server.utils.JsonRpcMessages.EXPECTED_TOOLS_CALL;
import static io.micronaut.mcp.server.utils.JsonRpcMessages.EXPECTED_TOOLS_LIST;
import static io.micronaut.mcp.server.utils.JsonRpcMessages.TOOLS_CALL;
import static io.micronaut.mcp.server.utils.JsonRpcMessages.TOOLS_LIST;
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

        @Singleton
        McpStatelessServerFeatures.AsyncToolSpecification getAlertsTools() {
            McpSchema.Tool tool = new McpSchema.Tool("fenEvaluation",
                "Evaluate a chess position using a FEN string.", FEN_EVALUATION_SCHEMA);
            return new McpStatelessServerFeatures.AsyncToolSpecification(tool,
                (exchange, arguments) -> {
                    return Mono.just(new McpSchema.CallToolResult("+0.27", false));
                }
            );
        }
    }

}
