from micronaut.context.annotation import Requires
# tag::imports[]
from typing import Annotated

from jakarta.inject import Singleton
from micronaut.mcp.annotations import Prompt, PromptArg
# end::imports[]


@Requires(property="spec.name", value="PromptsTest")
# tag::clazz[]
@Singleton
class Prompts:
    @Prompt(name="chess-statistics", description="Displays statistics for chess games")
    def prompt(self, name: Annotated[str, PromptArg(description="Player Name")]) -> str:
        """
        :return: Chess statistics
        """
        return f"You generate chess statistics for {name} ...."
# end::clazz[]
