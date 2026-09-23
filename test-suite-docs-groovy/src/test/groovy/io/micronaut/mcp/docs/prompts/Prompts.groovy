package io.micronaut.mcp.docs.prompts
/*
//tag::fakepackage[]
package example.micronaut

//end::fakepackage[]
*/
import io.micronaut.context.annotation.Requires
//tag::imports[]
import io.micronaut.mcp.annotations.Prompt
import io.micronaut.mcp.annotations.PromptArg
import jakarta.inject.Singleton

//end::imports[]

@Requires(property = "spec.name", value = "PromptsSpec")
//tag::clazz[]
@Singleton
class Prompts {
    /**
     *
     * @return Chess statistics
     */
    @Prompt(name = "chess-statistics", description = "Displays statistics for chess games")
    String prompt(@PromptArg(description = "Player Name") String name) {
        String.format("You generate chess statistics for %s ....", name)
    }
}
//end::clazz[]
