package io.micronaut.mcp.client.javasdk;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

@MicronautTest
class PromptCompletionsTest {

    @Test
    void classPathPromptsRegisterEmptyCompletions(McpSyncClient client) {

        McpSchema.CompleteRequest completeRequest = new McpSchema.CompleteRequest(new McpSchema.PromptReference("introspection-testing"),
            new McpSchema.CompleteRequest.CompleteArgument("className", "Bo"));
        McpSchema.CompleteResult completeResult = assertDoesNotThrow(() -> client.completeCompletion(completeRequest));
        assertEquals(new McpSchema.CompleteResult.CompleteCompletion(Collections.emptyList(), 0, false), completeResult.completion());
    }
}
