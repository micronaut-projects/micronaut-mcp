package io.micronaut.mcp.docs.completions.resources

//tag::imports[]
import io.micronaut.context.annotation.Requires
import io.micronaut.mcp.annotations.ResourceTemplate
import jakarta.inject.Singleton

//end::imports[]
@Requires(property = "spec.name", value = "MyResourceCompletionsTest")
//tag::clazz[]
@Singleton
class MyResourceTemplates {
    @ResourceTemplate(
        uriTemplate = "file:///home/user/documents/{fileName}",
        name = "userDocument",
        title = "User Document"
    )
    fun ref(fileName: String): String {
        return when (fileName) {
            "report.pdf" -> "Report PDF"
            "data.csv" -> "Data CSV"
            "notes.txt" -> "Notes TXT"
            else -> ""
        }
    }
}
//end::clazz[]
