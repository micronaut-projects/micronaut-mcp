package io.micronaut.mcp.docs.prompts

/*
//tag::fakepackage[]
package example.micronaut

//end::fakepackage[]
 */
//tag::imports[]
import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Requires
import io.modelcontextprotocol.common.McpTransportContext
import io.modelcontextprotocol.server.McpStatelessServerFeatures
import io.modelcontextprotocol.spec.McpSchema
import jakarta.inject.Singleton

//end::imports[]

@Requires(property = "spec.name", value = "PromptsFactorySpec")
//tag::clazz[]
@Factory
class PromptsFactory {
    @Singleton
    McpStatelessServerFeatures.SyncPromptSpecification prompt() {
        new McpStatelessServerFeatures.SyncPromptSpecification(
            new McpSchema.Prompt("chess-statistics", "Displays statistics for chess games",
                [new McpSchema.PromptArgument("name", "Player Name", true)]), { McpTransportContext ctx, McpSchema.GetPromptRequest req ->
            Object playerNameObj = req.arguments().get("name")
            String playerName = playerNameObj != null ? playerNameObj.toString() : ""
            McpSchema.TextContent assistantContent = new McpSchema.TextContent("You generate chess statistics for ${playerName} ....".toString())
            McpSchema.PromptMessage assistantMessage = new McpSchema.PromptMessage(McpSchema.Role.ASSISTANT, assistantContent)
            new McpSchema.GetPromptResult("Chess statistics", [assistantMessage], null)
        })
    }
}
//end::clazz[]
