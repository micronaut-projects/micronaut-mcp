package io.micronaut.mcp.docs.completions.prompts

import io.micronaut.context.annotation.Requires
import io.micronaut.mcp.annotations.Prompt
import io.micronaut.mcp.annotations.PromptArg
import jakarta.inject.Singleton

/**
 * The prompt completed by [MyPromptsCompletions]: the prompt name and the argument name match.
 */
@Requires(property = "spec.name", value = "MyPromptsCompletionsTest")
@Singleton
class MyPrompts {
    @Prompt(name = "code_review",
        title = "Request Code Review",
        description = "Asks the LLM to analyze code quality and suggest improvements")
    fun codeReview(@PromptArg(description = "The code to review") code: String,
                   @PromptArg(description = "The programming language") language: String): String {
        return "Please review this $language code: $code"
    }
}
