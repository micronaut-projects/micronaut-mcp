package example.micronaut.prompts;

import io.micronaut.context.annotation.Factory;
import io.modelcontextprotocol.server.McpStatelessServerFeatures;
import io.modelcontextprotocol.server.McpTransportContext;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

@Factory
class PromptsFactory {


    @Singleton
    McpStatelessServerFeatures.SyncPromptSpecification createPromptSpecification() {
        String name = "chess-statistics";
        String title = "Displays statistics for the 2024 World Chess Championship";
        String description = """
        Show the statistics for the 2024 World Chess Championship for both players.
        For each players it shows win, loss and draw with white and black""";
        List<McpSchema.PromptArgument> arguments =  new ArrayList<>();
        McpSchema.Prompt prompt = new McpSchema.Prompt(name, title, description, null, null);
        return new McpStatelessServerFeatures.SyncPromptSpecification(prompt, new BiFunction<McpTransportContext, McpSchema.GetPromptRequest, McpSchema.GetPromptResult>() {
            @Override
            public McpSchema.GetPromptResult apply(McpTransportContext mcpTransportContext, McpSchema.GetPromptRequest getPromptRequest) {
                McpSchema.TextContent assistantContent = new McpSchema.TextContent("""
                    You produce stats given a set of PGN Games.
                    You can obtain the 2024 World Chess championship via the resource with uri pgn://wcc2024
""");
                McpSchema.PromptMessage assistantMessage = new McpSchema.PromptMessage(McpSchema.Role.ASSISTANT, assistantContent);
                McpSchema.TextContent userContent = new McpSchema.TextContent("""
                    Give me the 2024 games world chess championship statistics by user. For each player, I want to see statistic per color white and black. For color, I want to see Wins losses and Draws.
                    I want to see also the statistics by Opening. Separate the Opening statistics also by color.
                    I just want you to output performance by color and performance by opening. You should give me the openings by ECO Code and the opening name""");
                McpSchema.PromptMessage userMessage = new McpSchema.PromptMessage(McpSchema.Role.USER, assistantContent);
                return new McpSchema.GetPromptResult("2024 World Chess Championship statistics", List.of(assistantMessage, userMessage), null);
            }
        });
    }


}
