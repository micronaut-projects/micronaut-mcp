package io.micronaut.mcp.docs.completions.prompts;

import io.micronaut.context.annotation.Requires;
import io.micronaut.mcp.annotations.Prompt;
import io.micronaut.mcp.annotations.PromptArg;
import jakarta.inject.Singleton;

/**
 * The prompt completed by {@link MyPromptsCompletions}: the prompt name and the argument name match.
 */
@Requires(property = "spec.name", value = "MyPromptsCompletionsTest")
@Singleton
class MyPrompts {
    @Prompt(name = "code_review",
        title = "Request Code Review",
        description = "Asks the LLM to analyze code quality and suggest improvements")
    String codeReview(@PromptArg(description = "The code to review") String code,
                      @PromptArg(description = "The programming language") String language) {
        return "Please review this " + language + " code: " + code;
    }
}
