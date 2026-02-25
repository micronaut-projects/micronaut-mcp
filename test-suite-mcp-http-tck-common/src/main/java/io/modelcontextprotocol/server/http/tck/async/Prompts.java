package io.modelcontextprotocol.server.http.tck.async;

import org.jspecify.annotations.NonNull;
import io.micronaut.mcp.annotations.Prompt;
import io.micronaut.mcp.annotations.PromptArg;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Singleton;

import java.util.List;

import static io.modelcontextprotocol.spec.McpSchema.Role.USER;

@Singleton
class Prompts {
    @Prompt(
        name = "code_review",
        title = "Request Code Review",
        description = "Asks the LLM to analyze code quality and suggest improvements"
    )
    McpSchema.GetPromptResult codeReview(@PromptArg(description = "The code to review") @NonNull String code) {
        return new McpSchema.GetPromptResult("Code review prompt",
            List.of(new McpSchema.PromptMessage(USER, new McpSchema.TextContent("Please review this Python code"))));
    }
}
