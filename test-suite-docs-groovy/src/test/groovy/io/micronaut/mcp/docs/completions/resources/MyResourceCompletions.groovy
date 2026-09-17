package io.micronaut.mcp.docs.completions.resources

//tag::imports[]
import io.micronaut.context.annotation.Requires
import io.micronaut.mcp.annotations.ResourceCompletion
import jakarta.inject.Singleton

//end::imports[]
@Requires(property = "spec.name", value = "MyResourceCompletionsSpec")
//tag::clazz[]
@Singleton
class MyResourceCompletions {
    @ResourceCompletion(uri = "file:///home/user/documents/{fileName}")
    List<String> resourcesCompletions(String fileName) {
        [
            "report.pdf",
            "data.csv",
            "notes.txt"
        ].findAll { String name -> name.startsWith(fileName) }
    }
}
//end::clazz[]
