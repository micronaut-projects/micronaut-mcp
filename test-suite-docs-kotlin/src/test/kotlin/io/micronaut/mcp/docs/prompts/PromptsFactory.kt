package io.micronaut.mcp.docs.prompts

/*
//tag::fakepackage[]
package example.micronaut

//end::fakepackage[]
 */
//tag::imports[]
import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Requires
import io.modelcontextprotocol.server.McpStatelessServerFeatures
import io.modelcontextprotocol.spec.McpSchema
import jakarta.inject.Singleton

//end::imports[]

@Requires(property = "spec.name", value = "PromptsFactoryTest")
//tag::clazz[]
@Factory
class PromptsFactory {
    @Singleton
    fun prompt(): McpStatelessServerFeatures.SyncPromptSpecification {
        return McpStatelessServerFeatures.SyncPromptSpecification(
            McpSchema.Prompt("chess-statistics", "Displays statistics for chess games",
                listOf(McpSchema.PromptArgument("name", "Player Name", true)))) { _, req ->
            val playerName = req.arguments()["name"]?.toString() ?: ""
            val assistantContent = McpSchema.TextContent("You generate chess statistics for $playerName ....")
            val assistantMessage = McpSchema.PromptMessage(McpSchema.Role.ASSISTANT, assistantContent)
            McpSchema.GetPromptResult("Chess statistics", listOf(assistantMessage), null)
        }
    }
}
//end::clazz[]
