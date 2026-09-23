package io.micronaut.mcp.docs.completions.resources

//tag::imports[]
import io.micronaut.context.annotation.Requires
import io.micronaut.mcp.annotations.ResourceTemplate
import jakarta.inject.Singleton

//end::imports[]
@Requires(property = "spec.name", value = "MyResourceCompletionsSpec")
//tag::clazz[]
@Singleton
class MyResourceTemplates {
    @ResourceTemplate(
        uriTemplate = "file:///home/user/documents/{fileName}",
        name = "userDocument",
        title = "User Document"
    )
    String ref(String fileName) {
        if (fileName == "report.pdf") {
            return "Report PDF"
        } else if (fileName == "data.csv") {
            return "Data CSV"
        } else if (fileName == "notes.txt") {
            return "Notes TXT"
        }
        ""
    }
}
//end::clazz[]
