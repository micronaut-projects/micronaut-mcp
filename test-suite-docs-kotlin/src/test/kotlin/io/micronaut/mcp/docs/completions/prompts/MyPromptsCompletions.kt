package io.micronaut.mcp.docs.completions.prompts

//tag::imports[]
import io.micronaut.context.annotation.Requires
import io.micronaut.mcp.annotations.PromptCompletion
import jakarta.inject.Singleton

//end::imports[]
@Requires(property = "spec.name", value = "MyPromptsCompletionsTest")
//tag::clazz[]
@Singleton
class MyPromptsCompletions {
    @PromptCompletion(name = "code_review")
    fun languages(language: String?): List<String> {
        if (language != null && language.startsWith("py")) {
            return listOf("python", "pytorch", "pyside")
        }
        return emptyList()
    }
}
//end::clazz[]
