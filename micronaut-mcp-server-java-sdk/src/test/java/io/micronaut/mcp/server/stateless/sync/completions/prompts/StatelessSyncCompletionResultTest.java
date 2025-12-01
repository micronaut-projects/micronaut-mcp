package io.micronaut.mcp.server.stateless.sync.completions.prompts;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import org.jspecify.annotations.NonNull;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.mcp.annotations.Prompt;
import io.micronaut.mcp.annotations.PromptArg;
import io.micronaut.mcp.annotations.PromptCompletion;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Singleton;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.util.Collections;
import java.util.List;

import static io.micronaut.mcp.server.utils.JsonRpcMessages.PROMPT_COMPLETION_REQUEST;
import static io.micronaut.mcp.server.utils.JsonRpcMessages.PROMPT_COMPLETION_RESPONSE;
import static io.modelcontextprotocol.spec.McpSchema.Role.USER;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@MicronautTest
@Property(name = "spec.name", value = "StatelessSyncCompletionResultTest")
class StatelessSyncCompletionResultTest {

    @Test
    void promptCompletion(@Client("/") HttpClient httpClient) throws JSONException {
        BlockingHttpClient client = httpClient.toBlocking();
        HttpRequest<?> req = HttpRequest.POST("/mcp", PROMPT_COMPLETION_REQUEST);
        HttpResponse<String> response = assertDoesNotThrow(() -> client.exchange(req, String.class));
        assertEquals(HttpStatus.OK, response.getStatus());
        String responseJson = response.body();
        JSONAssert.assertEquals(PROMPT_COMPLETION_RESPONSE, responseJson, true);
    }

    @Requires(property = "spec.name", value = "StatelessSyncCompletionResultTest")
    @Singleton
    static class MyCompletions {
        @PromptCompletion(name = "code_review")
        McpSchema.CompleteResult languages(McpSchema.CompleteRequest.CompleteArgument arg) {
            if (arg.name() != null && arg.name().equals("language") && arg.value() != null && arg.value().startsWith("py")) {
                return completeResult(List.of("python", "pytorch", "pyside"));
            }
            return completeResult(Collections.emptyList());
        }
    }

    private static McpSchema.CompleteResult completeResult(List<String> values) {
        return new McpSchema.CompleteResult(new McpSchema.CompleteResult.CompleteCompletion(values, values.size(), false));
    }

    @Requires(property = "spec.name", value = "StatelessSyncCompletionResultTest")
    @Singleton
    static class MyPrompts {
        @Prompt(
            name = "code_review",
            title = "Request Code Review",
            description = "Asks the LLM to analyze code quality and suggest improvements"
        )
        McpSchema.GetPromptResult codeReview(@PromptArg(description = "The code to review") @NonNull String code,
                                             @NonNull String language ) {
            return new McpSchema.GetPromptResult("Code review prompt",
                List.of(new McpSchema.PromptMessage(USER, new McpSchema.TextContent("Please review this Python code"))));
        }
    }

}
