# tag::imports[]
from jakarta.inject import Singleton
from micronaut.context.annotation import Requires
from micronaut.mcp.annotations import PromptCompletion
# end::imports[]


@Requires(property="spec.name", value="MyPromptsCompletionsTest")
# tag::clazz[]
@Singleton
class MyPromptsCompletions:
    @PromptCompletion(name="code_review")
    def languages(self, language: str | None) -> list[str]:
        if language is not None and language.startswith("py"):
            return ["python", "pytorch", "pyside"]
        return []
# end::clazz[]
