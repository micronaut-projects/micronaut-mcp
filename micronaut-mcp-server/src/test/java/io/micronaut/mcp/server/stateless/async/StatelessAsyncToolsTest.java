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
import jakarta.inject.Singleton;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import reactor.core.publisher.Mono;
import java.util.List;
import java.util.Map;

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
        @Singleton
        McpStatelessServerFeatures.AsyncToolSpecification getAlertsTools() {
            McpSchema.JsonSchema fenSchema = new McpSchema.JsonSchema("string", null,null, null, null, null);
            McpSchema.JsonSchema inputSchema = new McpSchema.JsonSchema("object", Map.of("fen", fenSchema), List.of("fen"), null, null, null);
            return McpStatelessServerFeatures.AsyncToolSpecification.builder()
                .tool(McpSchema.Tool.builder()
                .name("fenEvaluation")
                .description("Evaluate a chess position using a FEN string.")
                .inputSchema(inputSchema)
                .build())
                .callHandler((exchange, arguments) -> Mono.just(new McpSchema.CallToolResult("+0.12", false)))
                .build();
        }
    }

}
