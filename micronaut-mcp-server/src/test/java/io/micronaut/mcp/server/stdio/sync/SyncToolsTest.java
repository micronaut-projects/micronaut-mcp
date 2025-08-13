package io.micronaut.mcp.server.stdio.sync;

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
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static io.micronaut.mcp.server.utils.JsonRpcMessages.EXPECTED_TOOLS_CALL;
import static io.micronaut.mcp.server.utils.JsonRpcMessages.EXPECTED_TOOLS_LIST;
import static io.micronaut.mcp.server.utils.JsonRpcMessages.INITIALIZE;
import static io.micronaut.mcp.server.utils.JsonRpcMessages.INITIALIZED;
import static io.micronaut.mcp.server.utils.JsonRpcMessages.TOOLS_CALL;
import static io.micronaut.mcp.server.utils.JsonRpcMessages.TOOLS_LIST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Property(name = "spec.name", value = "SyncToolsTest")
@Property(name = "micronaut.mcp.server.type", value = "SYNC")
@MicronautTest(startApplication = false)
class SyncToolsTest {
    @Inject
    SyncInitializeTestFactory factory;

    @Test
    void syncTools() throws JSONException, IOException, ExecutionException, InterruptedException, TimeoutException {
        factory.stdio.sendRequest(INITIALIZE);
        Thread.sleep(TimeUnit.SECONDS.toMillis(1));
        factory.stdio.sendRequest(INITIALIZED);
        factory.stdio.sendRequest(TOOLS_LIST);
        Thread.sleep(TimeUnit.SECONDS.toMillis(1));
        factory.stdio.sendRequest(TOOLS_CALL);
        Thread.sleep(TimeUnit.SECONDS.toMillis(1));
        List<String> responses = factory.stdio.readResponses();
        assertEquals(3, responses.size());
        String responseJson = responses.get(1);
        assertNotNull(responseJson);
        JSONAssert.assertEquals(EXPECTED_TOOLS_LIST, responseJson, true);
        String toolCallResponse = responses.get(2);
        JSONAssert.assertEquals(EXPECTED_TOOLS_CALL, toolCallResponse, true);
    }

    @Requires(property = "spec.name", value = "SyncToolsTest")
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

    @Requires(property = "spec.name", value = "SyncToolsTest")
    @Factory
    static class FenEvaluationTool {
        @Singleton
        McpServerFeatures.SyncToolSpecification getAlertsTools() {
            McpSchema.JsonSchema fenSchema = new McpSchema.JsonSchema("string", null,null, null, null, null);
            McpSchema.JsonSchema inputSchema = new McpSchema.JsonSchema("object", Map.of("fen", fenSchema), List.of("fen"), null, null, null);
            return McpServerFeatures.SyncToolSpecification.builder()
                .tool(McpSchema.Tool.builder()
                .name("fenEvaluation")
                .description("Evaluate a chess position using a FEN string.")
                .inputSchema(inputSchema)
                .build())
                .callHandler((exchange, arguments) -> new McpSchema.CallToolResult("+0.27", false))
                .build();
        }
    }

}
