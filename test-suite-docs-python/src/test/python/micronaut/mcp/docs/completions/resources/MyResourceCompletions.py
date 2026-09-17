# tag::imports[]
from jakarta.inject import Singleton
from micronaut.context.annotation import Requires
from micronaut.mcp.annotations import ResourceCompletion
# end::imports[]


@Requires(property="spec.name", value="MyResourceCompletionsTest")
# tag::clazz[]
@Singleton
class MyResourceCompletions:
    @ResourceCompletion(uri="file:///home/user/documents/{fileName}")
    def resources_completions(self, fileName: str) -> list[str]:
        return [name for name in ["report.pdf", "data.csv", "notes.txt"] if name.startswith(fileName)]
# end::clazz[]
