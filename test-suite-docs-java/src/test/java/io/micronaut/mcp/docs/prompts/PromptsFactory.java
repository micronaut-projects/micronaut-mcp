package io.micronaut.mcp.docs.prompts;

/*
//tag::fakepackage[]
package example.micronaut;

//end::fakepackage[]
 */
//tag::imports[]
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;
import io.modelcontextprotocol.server.McpStatelessServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Singleton;

import java.util.List;

//end::imports[]

@Requires(property = "spec.name", value = "PromptsFactoryTest")
//tag::clazz[]
@Factory
class PromptsFactory {
    @Singleton
    McpStatelessServerFeatures.SyncPromptSpecification prompt() {
        return new McpStatelessServerFeatures.SyncPromptSpecification(
            new McpSchema.Prompt("chess-statistics", "Displays statistics for chess games",
                List.of(new McpSchema.PromptArgument("name", "Player Name", true))), (ctx, req) -> {
            Object playerNameObj = req.arguments().get("name");
            String playerName = playerNameObj != null ? playerNameObj.toString() : "";
            McpSchema.TextContent assistantContent = new McpSchema.TextContent(String.format("""
                                        You generate chess statistics for %s ....""", playerName));
            McpSchema.PromptMessage assistantMessage = new McpSchema.PromptMessage(McpSchema.Role.ASSISTANT, assistantContent);
            return new McpSchema.GetPromptResult("Chess statistics", List.of(assistantMessage), null);
        });
    }
}
//end::clazz[]
