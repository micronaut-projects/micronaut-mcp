package io.micronaut.mcp.docs.completions.resources

//tag::imports[]
import io.micronaut.context.annotation.Requires
import io.micronaut.mcp.annotations.ResourceCompletion
import jakarta.inject.Singleton

//end::imports[]
@Requires(property = "spec.name", value = "MyResourceCompletionsTest")
//tag::clazz[]
@Singleton
class MyResourceCompletions {
    @ResourceCompletion(uri = "file:///home/user/documents/{fileName}")
    fun resourcesCompletions(fileName: String): List<String> {
        return listOf(
                "report.pdf",
                "data.csv",
                "notes.txt"
            )
            .filter { name -> name.startsWith(fileName) }
    }
}
//end::clazz[]
