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

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static io.micronaut.mcp.server.utils.JsonRpcMessages.EXPECTED_PROMPTS;
import static io.micronaut.mcp.server.utils.JsonRpcMessages.INITIALIZE;
import static io.micronaut.mcp.server.utils.JsonRpcMessages.INITIALIZED;
import static io.micronaut.mcp.server.utils.JsonRpcMessages.PROMPTS_LIST;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Property(name = "micronaut.mcp.server.info.name", value="world-chess-championship-2024-pgn")
@Property(name = "micronaut.mcp.server.info.version", value="0.0.1")
@Property(name = "micronaut.mcp.server.type", value = "SYNC")
@Property(name = "spec.name", value = "SyncPromptsTest")
@MicronautTest
class SyncPromptsTest {
    @Inject
    ReplacementMcpServerTransportProviderFactory factory;

    @SuppressWarnings("java:S2925")
    @Test
    void syncPrompts() throws IOException, InterruptedException, JSONException {
        factory.stdio.sendRequest(INITIALIZE);
        Thread.sleep(TimeUnit.SECONDS.toMillis(1));
        factory.stdio.sendRequest(INITIALIZED);
        Thread.sleep(TimeUnit.SECONDS.toMillis(1));
        factory.stdio.sendRequest(PROMPTS_LIST);
        Thread.sleep(TimeUnit.SECONDS.toMillis(1));
        List<String> responses = factory.stdio.readResponses();
        assertEquals(2, responses.size());
        String readJsonRpc = responses.get(1);
        JSONAssert.assertEquals(EXPECTED_PROMPTS, readJsonRpc, true);
    }


    @Requires(property = "spec.name", value = "SyncPromptsTest")
    @Factory
    static class AsyncPromptsFactory {
        @Singleton
        McpServerFeatures.SyncPromptSpecification prompt() {
            return new McpServerFeatures.SyncPromptSpecification(
                new McpSchema.Prompt("chess-statistics", "Displays statistics for chess games",
                    List.of(new McpSchema.PromptArgument("name", "Player Name", true))), (ex, req) -> {
                Object playerNameObj = req.arguments().get("name");
                String playerName = playerNameObj != null ? playerNameObj.toString() : "";
                McpSchema.TextContent assistantContent = new McpSchema.TextContent(String.format("""
                                        You generate chess statistics for %s ....""", playerName));
                McpSchema.PromptMessage assistantMessage = new McpSchema.PromptMessage(McpSchema.Role.ASSISTANT, assistantContent);
                return new McpSchema.GetPromptResult("Chess statistics", List.of(assistantMessage), null);
            });
        }
    }

    @Requires(property = "spec.name", value = "SyncPromptsTest")
    @Factory
    static class ReplacementMcpServerTransportProviderFactory implements AutoCloseable {
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
