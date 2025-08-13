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

import java.util.List;

import static io.micronaut.mcp.server.utils.JsonRpcMessages.EXPECTED_PROMPTS;
import static io.micronaut.mcp.server.utils.JsonRpcMessages.PROMPTS_LIST;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@Property(name = "micronaut.mcp.server.type", value = "STATELESS_SYNC")
@Property(name = "spec.name", value = "StatelessSyncPromptsTest")
@MicronautTest
class StatelessSyncPromptsTest {

    @Test
    void statelessSyncPromptsList(@Client("/") HttpClient httpClient) throws JSONException {
        BlockingHttpClient client = httpClient.toBlocking();
        HttpRequest<?> req = HttpRequest.POST("/mcp", PROMPTS_LIST);
        String result = assertDoesNotThrow(() -> client.retrieve(req));
        JSONAssert.assertEquals(EXPECTED_PROMPTS, result, true);
    }

    @Requires(property = "spec.name", value = "StatelessSyncPromptsTest")
    @Factory
    static class StatelessSyncPromptsFactory {
        @Singleton
        McpStatelessServerFeatures.SyncPromptSpecification prompt() {
            return new McpStatelessServerFeatures.SyncPromptSpecification(
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
}
