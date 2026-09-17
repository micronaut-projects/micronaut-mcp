from typing import Annotated

from jakarta.inject import Singleton
from micronaut.context.annotation import Requires
from micronaut.mcp.annotations import Prompt, PromptArg


@Requires(property="spec.name", value="MyPromptsCompletionsTest")
@Singleton
class MyPrompts:
    """The prompt completed by MyPromptsCompletions: the prompt name and the argument name match."""

    @Prompt(name="code_review",
            title="Request Code Review",
            description="Asks the LLM to analyze code quality and suggest improvements")
    def code_review(self,
                    code: Annotated[str, PromptArg(description="The code to review")],
                    language: Annotated[str, PromptArg(description="The programming language")]) -> str:
        return f"Please review this {language} code: {code}"
