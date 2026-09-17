package io.micronaut.mcp.docs.completions.prompts;

//tag::imports[]
import io.micronaut.context.annotation.Requires;
import io.micronaut.mcp.annotations.PromptCompletion;
import jakarta.inject.Singleton;

import java.util.Collections;
import java.util.List;

//end::imports[]
@Requires(property = "spec.name", value = "MyPromptsCompletionsTest")
//tag::clazz[]
@Singleton
class MyPromptsCompletions {
    @PromptCompletion(name = "code_review")
    List<String> languages(String language) {
        if (language != null && language.startsWith("py")) {
            return List.of("python", "pytorch", "pyside");
        }
        return Collections.emptyList();
    }
}
//end::clazz[]
