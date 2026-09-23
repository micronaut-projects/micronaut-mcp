package io.micronaut.mcp.docs.completions.prompts

//tag::imports[]
import io.micronaut.context.annotation.Requires
import io.micronaut.mcp.annotations.PromptCompletion
import jakarta.inject.Singleton

//end::imports[]
@Requires(property = "spec.name", value = "MyPromptsCompletionsSpec")
//tag::clazz[]
@Singleton
class MyPromptsCompletions {
    @PromptCompletion(name = "code_review")
    List<String> languages(String language) {
        if (language != null && language.startsWith("py")) {
            return ["python", "pytorch", "pyside"]
        }
        Collections.emptyList()
    }
}
//end::clazz[]
