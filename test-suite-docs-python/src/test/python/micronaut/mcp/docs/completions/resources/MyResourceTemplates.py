# tag::imports[]
from jakarta.inject import Singleton
from micronaut.context.annotation import Requires
from micronaut.mcp.annotations import ResourceTemplate
# end::imports[]


@Requires(property="spec.name", value="MyResourceCompletionsTest")
# tag::clazz[]
@Singleton
class MyResourceTemplates:
    @ResourceTemplate(
        uriTemplate="file:///home/user/documents/{fileName}",
        name="userDocument",
        title="User Document"
    )
    def ref(self, fileName: str) -> str:
        if fileName == "report.pdf":
            return "Report PDF"
        elif fileName == "data.csv":
            return "Data CSV"
        elif fileName == "notes.txt":
            return "Notes TXT"
        return ""
# end::clazz[]
