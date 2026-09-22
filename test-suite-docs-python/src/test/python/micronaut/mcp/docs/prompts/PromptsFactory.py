# tag::imports[]
from io.modelcontextprotocol.server import McpStatelessServerFeatures
from io.modelcontextprotocol.spec import McpSchema
from jakarta.inject import Singleton
from java.util import List
from micronaut.context.annotation import Factory, Requires
# end::imports[]


@Requires(property="spec.name", value="PromptsFactoryTest")
# tag::clazz[]
@Factory
class PromptsFactory:
    @Singleton
    def prompt(self) -> McpStatelessServerFeatures.SyncPromptSpecification:
        return McpStatelessServerFeatures.SyncPromptSpecification(
            McpSchema.Prompt("chess-statistics", "Displays statistics for chess games",
                             List.of(McpSchema.PromptArgument("name", "Player Name", True))),
            lambda ctx, req: self.get_prompt_result(req))

    def get_prompt_result(self, req: McpSchema.GetPromptRequest) -> McpSchema.GetPromptResult:
        player_name_obj = req.arguments().get("name")
        player_name = str(player_name_obj) if player_name_obj is not None else ""
        assistant_content = McpSchema.TextContent(f"You generate chess statistics for {player_name} ....")
        assistant_message = McpSchema.PromptMessage(McpSchema.Role.ASSISTANT, assistant_content)
        return McpSchema.GetPromptResult("Chess statistics", List.of(assistant_message), None)
# end::clazz[]
